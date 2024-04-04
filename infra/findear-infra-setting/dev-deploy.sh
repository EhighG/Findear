#!/bin/bash

echo "Deploy dev by Blue Green strategy"
echo "-----------------------------------------------"
echo "Step 1. check what server has to update"
# it will be get by script parameter
export server=$1
regex="^(main|batch|match)$"
if [[ ! $server =~ $regex ]]; then
  echo "	input value doesn't have to be deploy"
  exit 1
fi
echo "	server is *$server*"

echo "-----------------------------------------------"
echo "Step 2. define blue and green"
# check by docker compose or curl
export blue="$(docker compose ps | grep $server | awk '{print $1}' | grep -o '[0-9]')"
if [[ ! $blue =~ ^-?[0-9]+$ ]]; then
    echo "	There are servers executing."
	echo "	Define blue by random"
	blue=0
fi
if [[ -z $blue ]]; then
	echo "	There is no server. Define blue by random"	
	blue=0
fi
if [[ $blue -eq "0" ]]; then
	green=1
else
	green=0
fi
echo "	Blue is $blue! Green is $green!"

echo "-----------------------------------------------"
echo "Step 3. turn on green"
docker compose down $server-dev-$green
docker compose build $server-dev-$green
docker compose up -d $server-dev-$green

echo "-----------------------------------------------"
echo "Step 4. healthcheck green"
sleep 5
status=false
for i in {1..10}
do
	health=$(docker inspect $server-dev-$green | jq '.[0]["State"]["Health"]["Status"]')
	echo "green health is... $health"
	if [[ $health == "\"healthy\"" ]]; then
		status=true
		echo "healthy green!"
		break
	fi
	sleep 5
done
if [[ $status == 'false' ]]; then
	echo "	Green server healthcheck in docker failed."
	exit 1
fi
echo "	Green server healthcheck complete"

echo "-----------------------------------------------"
echo "Step 5. update proxy"
docker compose exec web sh /etc/nginx/conf.d/change-bg.sh $server $blue $green
docker compose exec web nginx -s reload

echo "-----------------------------------------------"
echo "Step 6. healthcheck green by proxy"
# use curl, and catch the response, check the value
# at least we have to check five times, for five seconds
status=false
for i in {1..5}
do
	path=$server
	if [[ $path == "main" ]]; then
		path="api"
	fi
	response=$(curl -sLk "https://j10a706.p.ssafy.io/$path/actuator/health" | grep UP | wc -l)
	if [[ $response -eq 1 ]]; then
		status=true
		break
	fi
	sleep 1
done

if [[ $status == 'false' ]]; then
	echo "	Green server healthcheck in proxy failed."
	exit 1
fi
echo "	Green server healthcheck complete"

echo "-----------------------------------------------"
echo "Step 7. terminate blue"
docker compose down $server-dev-$blue

echo "Deploy done!!!!!"
