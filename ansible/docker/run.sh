#!/usr/bin/env bash

if [ $# -ne 2 ] && [ $# -ne 4 ] ; then
    echo "Usage: $0 password1 password2 OR $0 password1 password2 access_key secret_key"
    exit 1
fi

skipTags=${skipTags=cron,local}
module=${module=all}
dns=${dns=192.168.10.11}

if [ "$module" == "all" ] || [ "$module" == "submitter" ] ; then
    if [ $# -ne 4 ]; then
    echo "you are trying to upgrade the whole system or only the submitter and it requires access_key and secret_key to aws as third and forth parameters"
    exit 1
    fi
fi

echo "Upgrading newman environment modules: ${module}, will skip ${skipTags} tags, with dns: ${dns}, with cmdToDo: ${cmdToDo}..."
#docker run -v `pwd`/../..:/newman --env pass1=$1 --env pass2=$2  -it newman/anisble
docker run --dns=${dns} --env pass1=$1 --env pass2=$2 --env pass3=$3 --env pass4=$4 --env skipTags=${skipTags} --env module=${module} --env cmdToDo="${cmdToDo}" -it newman/anisble /bin/bash -c "cd /newman && git pull && /newman/ansible/docker/upgrade-all.sh"