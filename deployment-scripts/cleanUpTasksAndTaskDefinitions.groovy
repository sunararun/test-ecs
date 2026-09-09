def call(String region, String cluster, String taskFamily, String credentialsId = 'aws config') {
    stage('Clean Up Previous Task Definitions & Tasks') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                echo "Starting safe purge of older task definitions and stopping old running tasks..."

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

                    # Force it to be treated explicitly as an array to avoid unrolling bugs
                    $activeArns = @($activeArns)

                    if ($activeArns.Count -gt 1) {
                        # Sort them so the highest/newest revision number is last
                        $sortedArns = $activeArns | Sort-Object
                        $newestArn = $sortedArns[-1].ToString().Trim()
                        
                        # CRUCIAL SAFETY: Find what definition the ECS service is CURRENTLY deployed to
                        # This prevents killing active tasks before the new deployment completes
                        Write-Output "Checking active service configuration..."
                        $servicesJson = aws ecs list-services --cluster $env:CLEANUP_CLUSTER --region $env:CLEANUP_REGION | ConvertFrom-Json
                        $serviceArns = @($servicesJson.serviceArns)
                        
                        $currentServiceArn = $serviceArns | Where-Object { $_ -match $env:CLEANUP_TASK_FAMILY } | Select-Object -First 1
                        $inUseArn = $newestArn
                        
                        if ($currentServiceArn) {
                            $serviceDesc = aws ecs describe-services --cluster $env:CLEANUP_CLUSTER --services $currentServiceArn --region $env:CLEANUP_REGION | ConvertFrom-Json
                            # Find the primary (currently deploying/active) task definition in the service
                            $inUseArn = $serviceDesc.services[0].taskDefinition.ToString().Trim()
                        }

                        # Filter out BOTH the newest registered ARN and the one currently processing traffic
                        $arnsToDeregister = $sortedArns | Where-Object { $_.ToString().Trim() -ne $newestArn -and $_.ToString().Trim() -ne $inUseArn }

                        Write-Output "Protecting newest registered version: $newestArn"
                        if ($inUseArn -ne $newestArn) {
                            Write-Output "Protecting active deployment version currently in use: $inUseArn"
                        }

                        # 2. Fetch all currently RUNNING tasks in the cluster for this family
                        $runningTasks = aws ecs list-tasks `
                            --cluster $env:CLEANUP_CLUSTER `
                            --family $env:CLEANUP_TASK_FAMILY `
                            --desired-status RUNNING `
                            --query "taskArns" `
                            --output json `
                            --region $env:CLEANUP_REGION | ConvertFrom-Json

                        $runningTasks = @($runningTasks)

                        # 3. Map out which running tasks belong to older definitions and stop them
                        if ($runningTasks.Count -gt 0) {
                            Write-Output "Analyzing $($runningTasks.Count) running tasks..."
                            foreach ($taskArn in $runningTasks) {
                                $taskJson = aws ecs describe-tasks `
                                    --cluster $env:CLEANUP_CLUSTER `
                                    --tasks $taskArn `
                                    --region $env:CLEANUP_REGION | ConvertFrom-Json

                                # FIX: Explicit array index [0] to drill into the task object
                                $taskDefArnOfRunningTask = $taskJson.tasks[0].taskDefinitionArn.ToString().Trim()

                                # SAFETY CHECK: Do not stop it if it matches the newest OR the currently active service definition
                                if ($taskDefArnOfRunningTask -ne $newestArn -and $taskDefArnOfRunningTask -ne $inUseArn) {
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
