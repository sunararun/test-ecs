def call(String region, String cluster, String taskFamily, String credentialsId = 'aws config') {
    stage('Clean Up Previous Task Definitions') {
        withCredentials([[
                                 $class: 'AmazonWebServicesCredentialsBinding',
                                 credentialsId: credentialsId,
                                 accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                 secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                         ]]) {
            script {
                echo "Starting cleanup of old task definition revisions..."

                env.CLEANUP_REGION      = region
                env.CLEANUP_TASK_FAMILY = taskFamily

                powershell '''
                    # 1. Fetch all ACTIVE task definition ARNs for this family
                    $activeArns = aws ecs list-task-definitions `
                        --family-prefix $env:CLEANUP_TASK_FAMILY `
                        --status ACTIVE `
                        --query "taskDefinitionArns" `
                        --output json `
                        --region $env:CLEANUP_REGION | ConvertFrom-Json

                    # Ensure it is treated as a clean array list
                    $activeArns = @($activeArns)

                    if ($activeArns.Count -gt 1) {
                        # Sort them numerically by revision (highest/newest revision number is last)
                        $sortedArns = $activeArns | Sort-Object
                        
                        # Protect the newest deployed version from being touched
                        $newestArn = $sortedArns[-1].ToString().Trim()
                        
                        # Target all older versions for deregistration
                        $arnsToDeregister = $sortedArns | Where-Object { $_.ToString().Trim() -ne $newestArn }

                        Write-Output "Protecting newest registered version: $newestArn"

                        # 2. Deregister the older task definition versions cleanly
                        foreach ($arn in $arnsToDeregister) {
                            $cleanArn = $arn.ToString().Trim()
                            Write-Output "Deregistering older definition: $cleanArn"
                            aws ecs deregister-task-definition --task-definition $cleanArn --region $env:CLEANUP_REGION
                        }
                        
                        Write-Output "Cleanup complete. Older definitions are now INACTIVE."
                    } else {
                        Write-Output "No older active task definitions found to clean up."
                    }
                '''
            }
        }
    }
}
return this
