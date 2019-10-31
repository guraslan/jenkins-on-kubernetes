podTemplate(yaml: """
apiVersion: v1
kind: Pod
metadata:
  name: kaniko
spec:
  containers:
  - name: busybox
    image: busybox
    command:
      - cat
    tty: true
  - name: helm
    image: alpine-helm
    command:
      - cat
  - name: maven
    image: maven:3.6.1-jdk-8-alpine
    command: 
      - cat
    tty: true
  - name: trivy
    image: aquasec/trivy
    command:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug-v0.13.0
    command:
      - /busybox/cat
    tty: true
    volumeMounts:
      - name: kaniko-secret
        mountPath: /secret
    env:
      - name: GOOGLE_APPLICATION_CREDENTIALS
        value: /secret/kaniko-secret.json
  restartPolicy: Never
  volumes:
    - name: kaniko-secret
      secret:
        secretName: kaniko-secret
"""
  )
   {

    node(POD_LABEL) {
        stage('Build a Maven project') {
            git 'https://github.com/jenkinsci/kubernetes-plugin.git'
            container('maven') {
                stage('Build a Maven project') {
                    sh 'mvn -B clean install'
                    sh 'echo "Uploading jar to nexus...."'
                }
            }
        }

        stage('Build image/push') {
            container('kaniko') {
                stage('Build image/push') {
                    sh 'Download the artifact from Nexus'
                    sh '/busybox/echo /kaniko/executor --build-arg VERSION=2.15.2 --dockerfile=Dockerfile --context=dir://helm --destination=gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO}'
                }
            }
        }

        stage('Scan image') {
            container('trivy') {
                stage('Scan image'){
                    sh '''
                    export VERSION=$(curl --silent "https://api.github.com/repos/aquasecurity/trivy/releases/latest" | grep '"tag_name":' | sed -E 's/.*"v([^"]+)".*/\1/')
                    wget https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz
                    tar zxvf trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz
                    ./trivy --exit-code 1 --severity CRITICAL,HIGH,MEDIUM --no-progress --auto-refresh gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO}
                    '''
                }
            }
        }

        stage ('Deploy to TEST') {
            container('helm') {
                sh '''
                echo "Get config for the environment TEST"
                helm update --install --namespace test --set-image=gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO} vf stable/vf 
                '''
            }
        }

        stage ('Run functional tests on TEST') {
            container('busybox') {
                sh 'echo "Running tests on TEST environment..."'
            }
        }

        stage ('Promote to QA') {
            milestone(1)
            input 'Deploy to QA?'
            milestone(2)
        }

        stage ('Deploy to QA') {
            container('helm') {
                sh '''
                echo "Get config for the environment QA"
                helm update --install --namespace staging --set-image=gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO} vf stable/vf 
                '''
            }
        }

        stage ('Run functional tests on QA') {
            container('busybox') {
                sh 'echo "Running tests on QA environment..."'
            }
        }

        stage ('Promote to PROD') {
            milestone(3)
            input 'Deploy to PROD?'
            milestone(4)
        }

        stage ('Deploy to PROD') {
            container('helm') {
                sh '''
                echo "Get config for the environment PROD"
                helm update --install --namespace prod --set-image=gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO} vf stable/vf 
                '''
            }
        }

        stage ('Run functional tests on PROD') {
            container('busybox') {
                sh 'echo "Running tests on PROD environment..."'
            }
        }

    }
}