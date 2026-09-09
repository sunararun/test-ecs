def call(String bucketName, String region) {
    echo "Locating *.env file for S3 upload..."

    // Get only the file name (e.g., dev.env)
    def matchedFiles = bat(script: "dir /b env\\*.env", returnStdout: true).trim()
    def fileName = matchedFiles.substring(matchedFiles.lastIndexOf('\n') + 1).trim()

    if (!fileName || fileName.contains("File Not Found")) {
        error "Error: No matching *.env file found to upload!"
    }

    // Windows path structure uses backslashes
    def fullPath = "env\\${fileName}"
    echo "Uploading ${fullPath} to S3 bucket as deployments/${fileName}..."

    withAWS(region: region, credentials: 'aws config') {
        s3Upload(
                file: fullPath,
                bucket: bucketName,
                path: "deployments/${fileName}"
        )
    }
    echo "Upload completed successfully!"
}
return this
