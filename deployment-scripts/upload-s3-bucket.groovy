// dep/jenkins2.groovy
def call(String bucketName, String region) {
    echo "Preparing to upload env/.env to S3 bucket: ${bucketName}..."

    withAWS(region: region, credentials: 'aws-credentials-id') {
        s3Upload(
                file: 'env/.env',         // Path updated to look inside env/ folder
                bucket: bucketName,
                path: 'deployments/.env'  // Destination path inside your S3 bucket
        )
    }
    echo "Upload completed successfully!"
}
return this