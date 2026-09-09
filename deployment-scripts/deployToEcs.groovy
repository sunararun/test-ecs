def call(String region, String cluster, String service, String taskFamily, String repositoryUri, String imageTag, String credentialsId = 'aws config') {
    stage('Deploy to AWS ECS') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                // Safely expose method parameters to the node execution environment context
                env.ECS_REGION        = region
                env.ECS_CLUSTER       = cluster
                env.ECS_SERVICE       = service
                env.ECS_TASK_FAMILY   = taskFamily
                env.ECS_NEW_IMAGE     = "${repositoryUri}:${imageTag}"

                // 1. Fetch the task definition and filter out wrapper fields
                powershell '''
                    aws ecs describe-task-definition --task-definition $env:ECS_TASK_FAMILY --query taskDefinition --region $env:ECS_REGION > task-def.json
                '''

                // 2. Process JSON, strip read-only metadata, and bind the clean new image tag
                powershell '''
                    $taskDef = Get-Content task-def.json -Raw | ConvertFrom-Json

                    # Remove ECS-managed read-only fields
                    $propertiesToRemove = @('taskDefinitionArn', 'revision', 'status', 'requiresAttributes', 'compatibilities', 'registeredAt', 'registeredBy')
                    foreach ($prop in $propertiesToRemove) {
                        $taskDef.PSObject.Properties.Remove($prop)
                    }

                    # Inject the clean, explicit new tag passed from the environment block
                    $taskDef.containerDefinitions[0].image = $env:ECS_NEW_IMAGE

                    # Convert back to clean single-line JSON
                    $cleanJson = $taskDef | ConvertTo-Json -Depth 100 -Compress

                    # Write file directly as raw ASCII bytes to eliminate BOM signatures entirely
                    [System.IO.File]::WriteAllText("$pwd\\new-task-def.json", $cleanJson, [System.Text.Encoding]::ASCII)
                '''

                // AUDIT STEP: Verify that the clean file does not contain "@sha256"
                echo "--- AUDITING GENERATED TASK DEFINITION ---"
                powershell '''
                    Get-Content new-task-def.json
                '''

                // 3. Register the new definition file
                powershell '''
                    aws ecs register-task-definition --cli-input-json file://new-task-def.json --region $env:ECS_REGION > registered-task.json
                '''

                // Dynamically fetch the newly generated revision number
                def newRevision = powershell(
                        script: '''
                        (Get-Content registered-task.json | ConvertFrom-Json).taskDefinition.revision
                    ''',
                        returnStdout: true
                ).trim()

                // Expose the calculated revision explicitly to the final command context
                env.ECS_NEW_REVISION = newRevision

                // 4. Force deployment of the service using the newly generated numeric revision
                powershell '''
                    aws ecs update-service `
                        --cluster $env:ECS_CLUSTER `
                        --service $env:ECS_SERVICE `
                        --task-definition "${env:ECS_TASK_FAMILY}:${env:ECS_NEW_REVISION}" `
                        --force-new-deployment `
                        --region $env:ECS_REGION
                '''
            }
        }
    }
}
