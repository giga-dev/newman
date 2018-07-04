#!/bin/bash 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

# Update /etc/hosts
for i in $(seq 0 $END); do 
	line=${lines[$i]}
	
    array=(${line})
    host=${array[0]}
	echo $host

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /etc/hosts /home/xap/hosts.backup.$(date +\"%Y%m%d_%H%M%S\")"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	else
		sshpass -p password scp -o StrictHostKeyChecking=no ../hosts xap@$host:/home/xap/hosts
		sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "mv /home/xap/hosts /etc/hosts ; echo 127.0.0.1 `hostname` >> /etc/hosts"'
	fi
done