def call(String bucketName, String region) {
    echo "Locating *.env files for S3 upload..."

    // Find all matching files in the env directory
    def files = findFiles(glob: 'env/*.env')

    // Verify that at least one file exists
    if (files.length == 0) {
        error "Error: No matching *.env files found in the env/ folder to upload!"
    }

    // Authenticate with AWS and loop through the discovered files
    withAWS(region: region, credentials: 'aws config') {
        files.each { file ->
            echo "Uploading ${file.path} to S3..."
            s3Upload(
                    file: file.path,                  // Dynamically passes the exact file (e.g., 'env/dev.env')
                    bucket: bucketName,
                    path: "deployments/${file.name}" // Dynamically names it in S3 (e.g., 'deployments/dev.env')
            )
        }
    }
    echo "Upload completed successfully!"
}
return this
