pipeline {
    agent any // Must be a Windows-based node since your scripts use 'bat' and 'powershell'

    environment {
        // Centralized Configuration Management
        // AWS ECR Variables
        AWS_ACCOUNT_ID  = '501591550267'
        IMAGE_NAME      = 'springboot-app'
        IMAGE_TAG       = "${env.BUILD_NUMBER}"

        ECR_REGISTRY    = '501591550267.dkr.ecr.ap-south-1.amazonaws.com'
        AWS_REGION      = 'ap-south-1'

        // ECS Deployment Variables
        ECS_CLUSTER     = 'my-test-cluster'
        ECS_SERVICE     = 'my-test-task-family-service-1'
        ECS_TASK_FAMILY = 'my-test-task-family'

        // Fixed URL formatting for the repository URI
        REPOSITORY_URI  = "${ECR_REGISTRY}/${IMAGE_NAME}"
        ECS_EXECUTION_ROLE_ARN = 'arn:aws:iam::501591550267:role/ecsTaskExecutionRole' // Replace with your exact role ARN

    } // <-- environment

    stages {
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
            }
        }// <-- This is the end of the Checkout Code stage // <-- stage('Checkout') // <-- stage('Checkout')

        stage('Build Docker Image') {
            steps {
                script {
                    // Dynamically load the local script file
                    def buildScript = load "deployment/buildDockerImage.groovy"

                    // Call the method with arguments
                    buildScript.call(env.IMAGE_NAME, env.IMAGE_TAG, env.ECR_REGISTRY)
                }
            }
        } // <-- stage('Build Docker Image')

        stage('Push to ECR') {
            steps {
                script {
                    def pushScript = load "deployment/pushToEcr.groovy"
                    pushScript.call(env.AWS_REGION, env.ECR_REGISTRY, env.IMAGE_NAME, env.IMAGE_TAG, env.AWS_CREDS_ID)
                }
            }
        } // <-- stage('Push to ECR')

        stage('Deploy to ECS') {
            steps {
                script {
                    def deployScript = load "deployment/deployToEcs.groovy"
                    deployScript.call(
                            env.AWS_REGION,
                            env.ECS_CLUSTER,
                            env.ECS_SERVICE,
                            env.ECS_TASK_FAMILY,
                            env.ECR_REGISTRY, // Used as the repositoryUri
                            env.IMAGE_TAG,
                            env.AWS_CREDS_ID
                    )
                }
            }
        } // <-- stage('Deploy to ECS')

        stage('Post-Deployment Cleanup') {
            steps {
                script {
                    def cleanupScript = load "deployment/cleanUpTasksAndTaskDefinitions.groovy"
                    cleanupScript.call(env.AWS_REGION, env.ECS_CLUSTER, env.ECS_TASK_FAMILY, env.AWS_CREDS_ID)
                }
            }
        } // <-- stage('Post-Deployment Cleanup')
    }   // <-- stages
}// <-- pipeline
