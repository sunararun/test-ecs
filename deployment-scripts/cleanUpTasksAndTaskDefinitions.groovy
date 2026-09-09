def call(String region, String cluster, String taskFamily, String credentialsId = 'aws config') {
    stage('Clean Up Previous Task Definitions & Tasks') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                echo "Starting precise cleanup of older tasks and task definitions..."

                env.CLEANUP_REGION      = region
                env.CLEANUP_CLUSTER     = cluster
                env.CLEANUP_TASK_FAMILY = taskFamily

                powershell '''
                    # 1. Fetch all ACTIVE task definition ARNs for this family
                    $activeArns = aws ecs list-task-definitions `
                        --family-prefix $env:CLEANUP_TASK_FAMILY `
                        --status ACTIVE `
                        --query "taskDefinitionArns" `
                        --output json `
                        --region $env:CLEANUP_REGION | ConvertFrom-Json

                    $activeArns = @($activeArns)

                    if ($activeArns.Count -gt 1) {
                        # Sort them so the highest/newest revision number is last
                        $sortedArns = $activeArns | Sort-Object
                        $newestArn = $sortedArns[-1].ToString().Trim()
                        
                        # Target older versions for deregistration
                        $arnsToDeregister = $sortedArns | Where-Object { $_.ToString().Trim() -ne $newestArn }

                        Write-Output "Protecting current active version: $newestArn"

                        # 2. Fetch all currently RUNNING tasks in the cluster for this family
                        $runningTasks = aws ecs list-tasks `
                            --cluster $env:CLEANUP_CLUSTER `
                            --family $env:CLEANUP_TASK_FAMILY `
                            --desired-status RUNNING `
                            --query "taskArns" `
                            --output json `
                            --region $env:CLEANUP_REGION | ConvertFrom-Json

                        $runningTasks = @($runningTasks)

                        # 3. Explicitly look inside running tasks and stop them ONLY if they use an old ARN
                        if ($runningTasks.Count -gt 0) {
                            Write-Output "Analyzing $($runningTasks.Count) running tasks..."
                            
                            foreach ($taskArn in $runningTasks) {
                                $taskJson = aws ecs describe-tasks `
                                    --cluster $env:CLEANUP_CLUSTER `
                                    --tasks $taskArn `
                                    --region $env:CLEANUP_REGION | ConvertFrom-Json

                                # Target the specific zero-indexed task returned from the AWS API array
                                $taskDefArnOfRunningTask = $taskJson.tasks[0].taskDefinitionArn.ToString().Trim()

                                # SAFETY: Only stop the task if it is running on an older revision
                                if ($taskDefArnOfRunningTask -ne $newestArn) {
                                    Write-Output "Stopping old task container ($taskDefArnOfRunningTask): $taskArn"
                                    
                                    aws ecs stop-task `
                                        --cluster $env:CLEANUP_CLUSTER `
                                        --task $taskArn `
                                        --reason "Jenkins pipeline triggered rolling rotation cleanup" `
                                        --region $env:CLEANUP_REGION
                                } else {
                                    Write-Output "Leaving primary healthy task running: $taskArn"
                                }
                            }
                        } else {
                            Write-Output "No running tasks found in the cluster to evaluate."
                        }

                        # 4. Deregister the older task definition versions (move them from ACTIVE to INACTIVE)
                        foreach ($arn in $arnsToDeregister) {
                            $cleanArn = $arn.ToString().Trim()
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
return this
