def call(String region, String cluster, String taskFamily, String credentialsId = 'aws config') {
    stage('Clean Up Previous Task Definitions & Tasks') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                echo "Starting purge of older task definitions and stopping old running tasks..."

                env.CLEANUP_REGION      = region
                env.CLEANUP_CLUSTER     = cluster
                env.CLEANUP_TASK_FAMILY = taskFamily

                powershell '''
                    $activeArns = aws ecs list-task-definitions `
                        --family-prefix $env:CLEANUP_TASK_FAMILY `
                        --status ACTIVE `
                        --query "taskDefinitionArns" `
                        --output json `
                        --region $env:CLEANUP_REGION | ConvertFrom-Json

                    if ($activeArns -and $activeArns.Count -gt 1) {
                        $sortedArns = $activeArns | Sort-Object
                        $newestArn = $sortedArns[-1].Trim()
                        $arnsToDeregister = $sortedArns | Select-Object -First ($sortedArns.Count - 1)

                        Write-Output "Keeping current active version intact: $newestArn"

                        $runningTasks = aws ecs list-tasks `
                            --cluster $env:CLEANUP_CLUSTER `
                            --family $env:CLEANUP_TASK_FAMILY `
                            --desired-status RUNNING `
                            --query "taskArns" `
                            --output json `
                            --region $env:CLEANUP_REGION | ConvertFrom-Json

                        if ($runningTasks -and $runningTasks.Count -gt 0) {
                            Write-Output "Analyzing $($runningTasks.Count) running tasks..."
                            foreach ($taskArn in $runningTasks) {
                                $taskJson = aws ecs describe-tasks `
                                    --cluster $env:CLEANUP_CLUSTER `
                                    --tasks $taskArn `
                                    --region $env:CLEANUP_REGION | ConvertFrom-Json

                                $taskDefArnOfRunningTask = $taskJson.tasks.taskDefinitionArn.Trim()

                                if ($taskDefArnOfRunningTask -ne $newestArn) {
                                    Write-Output "Found running container on old revision ($taskDefArnOfRunningTask). Stopping Task: $taskArn"
                                    aws ecs stop-task `
                                        --cluster $env:CLEANUP_CLUSTER `
                                        --task $taskArn `
                                        --reason "Jenkins pipeline triggered rolling rotation cleanup" `
                                        --region $env:CLEANUP_REGION
                                }
                            }
                        } else {
                            Write-Output "No running tasks found in the cluster to evaluate."
                        }

                        foreach ($arn in $arnsToDeregister) {
                            $cleanArn = $arn.Trim()
                            Write-Output "Deregistering older definition: $cleanArn"
                            aws ecs deregister-task-definition --task-definition $cleanArn --region $env:CLEANUP_REGION
                        }
                        Write-Output "Cleanup complete."
                    } else {
                        Write-Output "No older active task definitions found to clean up or rotate."
                    }
                '''
            }
        }
    }
}
return this // <-- CRUCIAL FIX
