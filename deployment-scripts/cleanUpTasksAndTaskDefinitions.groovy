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
                        $sortedArns = $activeArns | Sort-Object
                        $newestArn = $sortedArns[-1].ToString().Trim()

                        # 2. Fetch all currently RUNNING tasks in the cluster
                        $runningTasks = aws ecs list-tasks `
                            --cluster $env:CLEANUP_CLUSTER `
                            --family $env:CLEANUP_TASK_FAMILY `
                            --desired-status RUNNING `
                            --query "taskArns" `
                            --output json `
                            --region $env:CLEANUP_REGION | ConvertFrom-Json

                        $runningTasks = @($runningTasks)

                        # Build a protection list initialized with the newest version
                        $protectedArns = @($newestArn)

                        if ($runningTasks.Count -gt 0) {
                            Write-Output "Analyzing $($runningTasks.Count) running tasks to protect active definitions..."
                            foreach ($taskArn in $runningTasks) {
                                $taskJson = aws ecs describe-tasks `
                                    --cluster $env:CLEANUP_CLUSTER `
                                    --tasks $taskArn `
                                    --region $env:CLEANUP_REGION | ConvertFrom-Json

                                $taskDefArnOfRunningTask = $taskJson.tasks[0].taskDefinitionArn.ToString().Trim()
                                
                                if ($protectedArns -notcontains $taskDefArnOfRunningTask) {
                                    $protectedArns += $taskDefArnOfRunningTask
                                    Write-Output "Protecting active running version: $taskDefArnOfRunningTask"
                                }
                            }
                        }

                        # 3. Target versions that are NOT the newest AND NOT actively running
                        $arnsToDeregister = $sortedArns | Where-Object { $protectedArns -notcontains $_.ToString().Trim() }

                        # 4. Deregister ONLY truly unused older versions
                        if ($arnsToDeregister.Count -gt 0) {
                            foreach ($arn in $arnsToDeregister) {
                                $cleanArn = $arn.ToString().Trim()
                                Write-Output "Deregistering unused older definition: $cleanArn"
                                aws ecs deregister-task-definition --task-definition $cleanArn --region $env:CLEANUP_REGION
                            }
                        } else {
                            Write-Output "No unused older active task definitions available to clean up."
                        }
                        
                        Write-Output "Cleanup complete."
                    } else {
                        Write-Output "No older active task definitions found to clean up."
                    }
                '''
            }
        }
    }
}
return this
