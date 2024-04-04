#!/bin/bash

echo "initial setting"

source env.txt

echo 'install docker in hostOS'
apt install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $UBUNTU_VERSION stable"
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose docker-compose-plugin

systemctl enable docker
systemctl start docker

apt install -y openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

mkdir -p /var/jenkins_home
chown -R 1000:1000 /var/jenkins_home/
