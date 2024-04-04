#!/bin/bash

cp -f .vimrc ..

echo '도커 설치'
apt install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable"
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose docker-compose-plugin

systemctl enable docker
systemctl start docker

echo '젠킨스 설치'
mkdir -p /var/jenkins_home
chown -R 1000:1000 /var/jenkins_home/
sudo docker run --restart=on-failure --user='root' -p 3100:8080 -p 50000:50000 -v /var/jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock -d --name jenkins jenkins/jenkins:lts

# 젠컨에 필요한 파일 넣고 실행
# 도커, 노드, 자바 설치 필요
# settings direc에 전부 넣기
# .env 파일도 같이 여기에 들어가야 함
# application.yml도 직접 넣어주기
echo '젠킨스 컨테이너 설정'
mkdir -p /var/jenkins_home/settings
chmod 777 jenkins_settings.sh
cp jenkins_settings.sh /var/jenkins_home/settings
cp application.yml /var/jenkins_home/settings

docker exec jenkins sh /var/jenkins_home/settings/jenkins_settings.sh

#sql디비 설치
# 논리 볼륨 만들어서 겉에 놔두기
echo '디비 설치'
docker volume create data_volume
docker pull mysql:8.2.0
docker run -d -p 3200:3306 -e MYSQL_DATABASE=comeet -e MYSQL_USER=comeet -e MYSQL_PASSWORD=comeet -e MYSQL_ROOT_PASSWORD=root -v data_volume:/var/lib/mysql--name database mysql:8.2.0

# redis 디비 설치
docker pull redis:latest
docker run -d -p 3201:6379 --name=redis redis:latest

# 개발용 백 설치
# 초기 설정 파일 구동
# 재실행 파일도 필요
echo '개발용 백 설치'
docker pull ubuntu:jammy
chmod 777 back_settings.sh
chmod 777 back_execution.sh
cp back_settings.sh /var/jenkins_home/settings
cp back_execution.sh /var/jenkins_home/settings
docker run -td --link database:database -p 3002:8080 -v /var/jenkins_home/settings/:/settings --user='root' --name dev_back ubuntu:jammy
docker exec dev_back sh /settings/back_settings.sh

# 배포용 백 설치
# 초기 설정 파일 구동
# 재실행 파일도 필요
echo '배포용 백 설치'
docker run -td --link database:database -p 3003:8080 -v /var/jenkins_home/settings/:/settings --user='root' --name dep_back ubuntu:jammy
docker exec dep_back sh /settings/back_settings.sh

# 배포용 백 설치
# 초기 설정 파일 구동
# 재실행 파일도 필요
echo '배포용 백 설치'
docker run -td -p 3001:3000 -v /var/jenkins_home/settings/:/settings --user='root' --name front ubuntu:jammy
docker exec front sh /settings/front_settings.sh

# 배포용 웹 서버 설치
# 초기 설정 파일 구동
# 재실행 파일 필요
echo '배포용 웹 설치'
chmod 777 web_settings.sh
chmod 777 web_execution.sh
cp web_settings.sh /var/jenkins_home/settings
cp web_execution.sh /var/jenkins_home/settings
docker pull nginx:1.25.3
docker run -td --user=root -v /var/jenkins_home/settings:/settings -p 443:443-p 80:80 --name web nginx:1.25.3
docker exec web sh /settings/web_settings.sh
docker start web
