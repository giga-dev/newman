#!/bin/bash 
set +x
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

function killagents {
	trap "" HUP
	local supervisorpid=$(ps aux | grep supervisor | grep -v grep | awk '{ print $2 }')
	echo Killing supervisor [$supervisorpid]
	kill -9 $supervisorpid
	echo Killing Java processes
	killall -9 java

	echo Removing docker containers
	docker rm -f $(docker ps -aq)c
}



readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

# Update /etc/hosts
for i in $(seq 0 $END); do 
	line=${lines[$i]}
	
    array=(${line})
    host=${array[0]}
	echo "Trying $host"

	# sshpass -p password ssh xap@$host -C "mkdir -p ~/software/java && cd ~/software/java && ln -s /opt/jdk1.8.0_144 current"
	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$(typeset -f); killagents $1 > /tmp/killagents.out 2>&1 &"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	fi
done
