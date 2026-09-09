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

                // Safely expose method parameters to the node environment context
                env.CLEANUP_REGION      = region
                env.CLEANUP_CLUSTER     = cluster
                env.CLEANUP_TASK_FAMILY = taskFamily

                // Using a single-quoted raw string means zero Groovy escaping errors ($ stays $)
                powershell '''
                    # 1. Fetch all ACTIVE task definition ARNs for this family
                    $activeArns = aws ecs list-task-definitions `
                        --family-prefix $env:CLEANUP_TASK_FAMILY `
                        --status ACTIVE `
                        --query "taskDefinitionArns" `
                        --output json `
                        --region $env:CLEANUP_REGION | ConvertFrom-Json

                    if ($activeArns -and $activeArns.Count -gt 1) {
                        # Sort them so the highest/newest revision number is last
                        $sortedArns = $activeArns | Sort-Object

                        # Protect the newest deployed version
                        $newestArn = $sortedArns[-1].Trim()
                        $arnsToDeregister = $sortedArns | Select-Object -First ($sortedArns.Count - 1)

                        Write-Output "Keeping current active version intact: $newestArn"

                        # 2. Fetch all currently RUNNING tasks in the cluster for this family
                        $runningTasks = aws ecs list-tasks `
                            --cluster $env:CLEANUP_CLUSTER `
                            --family $env:CLEANUP_TASK_FAMILY `
                            --desired-status RUNNING `
                            --query "taskArns" `
                            --output json `
                            --region $env:CLEANUP_REGION | ConvertFrom-Json

                        # 3. Map out which running tasks belong to older definitions and stop them
                        if ($runningTasks -and $runningTasks.Count -gt 0) {
                            Write-Output "Analyzing $($runningTasks.Count) running tasks..."

                            foreach ($taskArn in $runningTasks) {
                                # Query task details as clean JSON to safely parse object paths
                                $taskJson = aws ecs describe-tasks `
                                    --cluster $env:CLEANUP_CLUSTER `
                                    --tasks $taskArn `
                                    --region $env:CLEANUP_REGION | ConvertFrom-Json

                                $taskDefArnOfRunningTask = $taskJson.tasks[0].taskDefinitionArn.Trim()

                                # If the task definition of this running task is NOT the newest one, stop it
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

                        # 4. Deregister the older task definition versions
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
