@echo off
set IMAGE_NAME=first-ecs-app
set CONTAINER_NAME=first-ecs-container

echo Building Docker image...
docker build -t %IMAGE_NAME% .

echo Stopping existing container if running...
docker stop %CONTAINER_NAME% 2>nul
docker rm %CONTAINER_NAME% 2>nul

echo Running container...
docker run --name %CONTAINER_NAME% -p 5000:5000 %IMAGE_NAME%
