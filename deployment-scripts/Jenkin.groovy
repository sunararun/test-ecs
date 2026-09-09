pipeline {
    agent any

    environment {
        S3_BUCKET = 'elasticbeanstalk-ap-south-1-501591550267'
        AWS_REGION = 'ap-south-1'
    } // <-- This is the end of the environment block

    stages {
        stage('Checkout Code') {
            steps {
                // This explicitly pulls your repo down into the workspace
                checkout scmGit(
                        branches: [[name: '*/main']],
                        extensions: [],
                        userRemoteConfigs: [[url: 'https://bitbucket1584@bitbucket.org/awseb1584/test-ecs.git']]
                )
            }
        } // <-- This is the end of the Checkout Code stage

        stage('Search .env File') {
            steps {
                script {
                    // Load and execute from the deployment-scripts folder
                    def searchScript = load 'deployment-scripts/search-env-file.groovy'
                    searchScript()
                }
            }
        } // <-- This is the end of the Search .env File stage

        stage('Upload to S3') {
            steps {
                script {
                    // Load and execute from the deployment-scripts folder
                    def uploadScript = load 'deployment-scripts/upload-s3-bucket.groovy'
                    uploadScript(env.S3_BUCKET, env.AWS_REGION)
                }
            }
        }

    } // <-- This is the end of the stages block
} // <-- This is the end of the pipeline block
