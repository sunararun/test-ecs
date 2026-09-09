def call() {
    echo "Searching for any .env file in the env\\ directory using Windows Batch..."

    // Check if any file matching *.env exists. returnStatus: true prevents Jenkins from failing if not found.
    def fileCheck = bat(script: "dir /b env\\*.env", returnStatus: true)

    if (fileCheck == 0) {
        // Fetch the file name cleanly
        def matchedFiles = bat(script: "dir /b env\\*.env", returnStdout: true).trim()
        // Strip out the Jenkins execution text that bat command outputs
        def cleanFileName = matchedFiles.substring(matchedFiles.lastIndexOf('\n') + 1).trim()

        echo "Success: Found environment file -> env/${cleanFileName}"
    } else {
        error "Error: No file matching *.env was found in the env\\ directory!"
    }
}
return this
