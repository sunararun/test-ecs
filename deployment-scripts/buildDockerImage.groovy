def call(String imageName, String imageTag, String registry) {
    stage('Build Docker Image') {
        env.BUILD_IMAGE_NAME = imageName
        env.BUILD_IMAGE_TAG  = imageTag
        env.BUILD_REGISTRY   = registry

        bat '''
            docker build -t %BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG% .
        '''
        bat '''
            docker tag %BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG% %BUILD_REGISTRY%/%BUILD_IMAGE_NAME%:%BUILD_IMAGE_TAG%
        '''
        echo "Docker image built successfully: ${imageName}:${imageTag}"
    }
}
return this // <-- CRUCIAL FIX
