podTemplate(containers: [
    containerTemplate(name: 'buildah', image: 'quay.io/buildah/stable:v1.11.3', ttyEnabled: true, command: 'cat'),
  ],
  volumes: [
      hostPathVolume(mountPath: '/var/lib/containers', hostPath: '/mnt')
  ]) {

    node(POD_LABEL) {

        stage('Create image with Buildah') {
            git url: 'https://github.com/alpine-docker/helm.git'
            container('buildah') {
                stage('Install buildah') {
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