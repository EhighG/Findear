server=$1
blue=$2
green=$3
echo "$server-$blue to $server-$green"
sed -i "s/dev-$blue/dev-$green/g" /etc/nginx/conf.d/${server}-dev.conf
