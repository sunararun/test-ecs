// dep/jenkins1.groovy
def call() {
    echo "Searching for .env file inside the 'env' directory..."

    // Check path inside the env folder
    def envExists = fileExists('env/.env')

    if (envExists) {
        echo "Success: env/.env file found!"
    } else {
        error "Error: .env file is missing from the 'env/' folder!"
    }
}
return this