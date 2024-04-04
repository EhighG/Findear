#!/bin/bash
CONTAINER="jenkins"
if [ -n "$1" ]; then
	echo "input inserted::: $1"
	CONTAINER=$1
fi
echo "delete $CONTAINER"
docker stop $CONTAINER
docker rm $CONTAINER
