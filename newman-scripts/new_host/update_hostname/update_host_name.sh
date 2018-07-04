#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

#value=`cat ${HOSTS_FILE}`

readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

for i in $(seq 0 $END); do 
    line=${lines[$i]}
	echo $line
    array=(${line})
    host=${array[0]}
    newhost=${array[1]}
    echo "Changing ${host} to ${newip}"

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "sudo hostnamectl set-hostname $newhost"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	else
		echo "Rebooting $host"
		sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "reboot"'
	fi

done
