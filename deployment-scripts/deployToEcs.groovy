def call(String region, String cluster, String service, String taskFamily, String repositoryUri, String imageTag, String credentialsId = 'aws config') {
    stage('Deploy to AWS ECS') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                env.ECS_REGION        = region
                env.ECS_CLUSTER       = cluster
                env.ECS_SERVICE       = service
                env.ECS_TASK_FAMILY   = taskFamily
                env.ECS_NEW_IMAGE     = "${repositoryUri}:${imageTag}"

                powershell '''
                    aws ecs describe-task-definition --task-definition $env:ECS_TASK_FAMILY --query taskDefinition --region $env:ECS_REGION > task-def.json
                '''

                powershell '''
                    $taskDef = Get-Content task-def.json -Raw | ConvertFrom-Json
                    $propertiesToRemove = @('taskDefinitionArn', 'revision', 'status', 'requiresAttributes', 'compatibilities', 'registeredAt', 'registeredBy')
                    foreach ($prop in $propertiesToRemove) {
                        $taskDef.PSObject.Properties.Remove($prop)
                    }
                    $taskDef.containerDefinitions.image = $env:ECS_NEW_IMAGE
                    $cleanJson = $taskDef | ConvertTo-Json -Depth 100 -Compress
                    [System.IO.File]::WriteAllText("$pwd\\new-task-def.json", $cleanJson, [System.Text.Encoding]::ASCII)
                '''

                echo "--- AUDITING GENERATED TASK DEFINITION ---"
                powershell '''Get-Content new-task-def.json'''

                powershell '''
                    aws ecs register-task-definition --cli-input-json file://new-task-def.json --region $env:ECS_REGION > registered-task.json
                '''

                def newRevision = powershell(
                        script: '''(Get-Content registered-task.json | ConvertFrom-Json).taskDefinition.revision''',
                        returnStdout: true
                ).trim()

                env.ECS_NEW_REVISION = newRevision

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
return this // <-- CRUCIAL FIX
