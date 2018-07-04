#!/bin/bash 
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../newman-hosts"
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

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /home/xap/newman-agent/newman-agent.sh /home/xap/newman-agent/newman-agent.sh.backup.$(date +\"%Y%m%d_%H%M%S\")"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	else
		sshpass -p password scp -o StrictHostKeyChecking=no ${DIR}/../../newman-agent/bin/newman-agent.sh xap@$host:/home/xap/newman-agent/newman-agent.sh
		sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "killall -9 java"'
	fi
done
