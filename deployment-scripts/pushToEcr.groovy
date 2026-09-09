def call(String region, String registry, String imageName, String imageTag, String credentialsId = 'aws config') {
    stage('Authorize & Push to AWS ECR') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            echo "Logging into AWS ECR..."

            // Safely expose method parameters to the node execution environment context
            env.ECR_REGION   = region
            env.ECR_REGISTRY = registry
            env.IMAGE_NAME   = imageName
            env.IMAGE_TAG    = imageTag

            // Single quotes preserve raw string semantics; Windows handles %ECR_...% variables cleanly
            bat '''
                aws ecr get-login-password --region %ECR_REGION% | docker login --username AWS --password-stdin %ECR_REGISTRY%
            '''

            bat '''
                docker push %ECR_REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%
            '''
        }
    }
}
