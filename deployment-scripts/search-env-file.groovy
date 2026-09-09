def call() {
    echo "Searching for any .env file in the env/ directory..."

    // Find files matching the pattern. This returns an array.
    def files = findFiles(glob: 'env/*.env')

    // Check if the array contains at least one file
    if (files.length > 0) {
        // Print the specific file name that was found (e.g., dev.env)
        echo "Success: Found environment file -> ${files[0].path}"
    } else {
        error "Error: No .env file matching *.env was found in the env/ directory!"
    }
}
return this