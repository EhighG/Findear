#!/bin/bash

# 1. check who is blue and green
tmp=$(lsof -i :8080)
if [ -z $tmp ]; then
	if [ -z $(lsof -i :8081) ]; then
		blue=8080
		green=8081
	else
		blue=8081
		green=8080
	fi
else
	blue=8080
	green=8081
fi
echo "current Blue is $blue"
echo "Green to execute next is $green"

