/**
 * This pipeline will build and deploy a Docker image with Kaniko
 * https://github.com/GoogleContainerTools/kaniko
 * without needing a Docker host
 *
 * You need to create a jenkins-docker-cfg secret with your docker config
 * as described in
 * https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-secret-in-the-cluster-that-holds-your-authorization-token
 */

podTemplate(yaml: """
apiVersion: v1
kind: Pod
metadata:
  name: kaniko
spec:
  containers:
  - name: golang
    image: golang:1.8.0
    command: 
      - cat
    tty: true
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
  ) {

  node(POD_LABEL) {
    stage('Build code') {
        container('golang') {
            sh """
               sleep 100000000
               """
        }
    }
    stage('Build image') {
      git 'https://github.com/guraslan/helm.git'
      container('kaniko') {
        sh '/kaniko/executor --build-arg VERSION=2.15.2 --dockerfile=Dockerfile --context=dir://helm --destination=gcr.io/stalwart-topic-257411/alpine-helm:${BUILD_NO}'
      }
    }
  }
}