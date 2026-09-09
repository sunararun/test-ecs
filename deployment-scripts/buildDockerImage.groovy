def call(String imageName, String imageTag, String registry) {
    stage('Build Docker Image') {
        steps {
            script {
                // Safely expose method parameters to the node execution environment context
                env.BUILD_IMAGE_NAME = imageName
                env.BUILD_IMAGE_TAG  = imageTag
                env.BUILD_REGISTRY   = registry

                // Raw string literals prevent Groovy string formatting issues on Windows runners
                bat '''
                    docker build -t %BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG% .
                '''

                bat '''
                    docker tag %BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG% %BUILD_REGISTRY%/%BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG%
                '''

                echo "Docker image built successfully: ${imageName}:${imageTag}"
            }
        }
    }
}