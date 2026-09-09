pipeline {
    agent any

    environment {
        S3_BUCKET = 'elasticbeanstalk-ap-south-1-501591550267'
        AWS_REGION = 'ap-south-1'
    } // <-- environment

    stages {
        stage('Checkout') {
            steps {
                // Fixed missing closing parenthesis at the very end of checkout
                checkout scmGit(
                        branches: [[name: '*/main']],
                        extensions: [],
                        userRemoteConfigs: [[url: 'https://bitbucket1584@bitbucket.org/awseb1584/test-ecs.git']]
                )
            }
        } // <-- Checkout stage

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        } // <-- Checkout Code stage

        stage('Search .env File') {
            steps {
                script {
                    // Load and execute search-env-file.groovy from the deployment-scripts folder
                    def searchScript = load 'deployment-scripts/search-env-file.groovy'
                    searchScript()
                }
            }
        } // <-- Search .env File stage

        stage('Upload to S3') {
            steps {
                script {
                    // Load and execute upload-s3-bucket.groovy from the deployment-scripts folder
                    def uploadScript = load 'deployment-scripts/upload-s3-bucket.groovy'
                    uploadScript(env.S3_BUCKET, env.AWS_REGION)
                }
            }
        } // <-- Upload to S3 stage     

    } // <-- stages

} // <-- pipeline