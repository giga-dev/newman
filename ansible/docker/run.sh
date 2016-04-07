#!/usr/bin/env bash

if [ $# -ne 2 ]; then
    echo "Usage: $0 password1 password2"
    exit 1
fi

skipTags=${skipTags=cron,local}
module=${module=all}
dns=${dns=192.168.10.11}

echo "Upgrading newman environment modules: ${module}, will skip ${skipTags} tags, with dns: ${dns}, with cmdToDo: ${cmdToDo}..."
#docker run -v `pwd`/../..:/newman --env pass1=$1 --env pass2=$2  -it newman/anisble
docker run --dns=${dns} --env pass1=$1 --env pass2=$2 --env skipTags=${skipTags} --env module=${module} --env cmdToDo=${cmdToDo} -it newman/anisble /bin/bash -c "cd /newman && git pull && /newman/ansible/docker/upgrade-all.sh"