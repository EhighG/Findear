#!/bin/bash


docker run -dt -p 3100:8080 -p 50000:50000 -v /var/jenkins_home:/var/jenkins_home --name jenkins test
