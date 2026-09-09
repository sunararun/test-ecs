def call(String bucketName, String region) {
    echo "Uploading env/.env to S3..."
    withAWS(region: region, credentials: 'aws config') {
        s3Upload(file: 'env/.env', bucket: bucketName, path: 'deployments/.env')
    }
}
return this