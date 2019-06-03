#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

function remote_command {
    crontab -l | sed "s/@reboot/#@reboot/" | crontab -
}

# Update /etc/hosts
for i in $(seq 0 $END); do 
	line=${lines[$i]}
	
    array=(${line})
    host=${array[0]}
	echo $host

    ## Backup jar file
	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$(declare -f remote_command) ; remote_command"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	fi
done
