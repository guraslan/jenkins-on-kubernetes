podTemplate(containers: [
    containerTemplate(name: 'buildah', image: 'centos:centos7', ttyEnabled: true, command: 'cat'),
  ]) {

    node(POD_LABEL) {

        stage('Create image with Buildah') {
            git url: 'https://github.com/alpine-docker/helm.git'
            container('buildah') {
                staged('Install buildah') {
                    sh """
                    yum -y install buildah
                    """
                }
                stage('Build alpine-helm image') {
                    sh """
                    buildah bud -f Dockerfile --build-arg VERSION=2.12.0 -t alpine-help:2.12.0 .
                    """
                }
            }
        }

    }
}