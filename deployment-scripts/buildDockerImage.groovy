def call(String imageName, String imageTag, String registry) {
    stage('Build Docker Image') {
        env.BUILD_IMAGE_NAME = imageName
        env.BUILD_IMAGE_TAG  = imageTag
        env.BUILD_REGISTRY   = registry

        // Build straight into the ECR tag structure to produce exactly one image
        bat '''
            docker build -t %BUILD_REGISTRY%/%BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG% .
        '''

        echo "Docker image successfully built with single ECR tag: ${registry}/${imageName}:${imageTag}"
    }
}
return this
