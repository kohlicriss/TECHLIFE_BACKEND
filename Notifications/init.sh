#!/bin/bash
IMAGE_NAME=notifications
VERSION_TAG=v1.0.0
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 435073375740.dkr.ecr.ap-south-1.amazonaws.com
docker build -t $IMAGE_NAME:$VERSION_TAG .
docker tag $IMAGE_NAME:$VERSION_TAG 435073375740.dkr.ecr.ap-south-1.amazonaws.com/hrms/$IMAGE_NAME:$VERSION_TAG
docker push 435073375740.dkr.ecr.ap-south-1.amazonaws.com/hrms/$IMAGE_NAME:$VERSION_TAG