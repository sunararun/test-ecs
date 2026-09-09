def call() {
    echo "Searching for .env file..."
    if (fileExists('env/*.env')) {
        echo "Success: env/*.env found!"
    } else {
        error "Error: .env file is missing from the env/ directory!"
    }
}
return this