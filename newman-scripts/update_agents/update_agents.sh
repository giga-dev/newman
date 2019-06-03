#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

export DATE_SUFFIX="$(date +%Y%m%d_%H%M%S)"

# Update /etc/hosts
for i in $(seq 0 $END); do 
	line=${lines[$i]}
	
    array=(${line})
    host=${array[0]}
	echo $host

    ## Backup jar file
	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /home/xap/newman-agent/newman-agent-1.0.jar /home/xap/newman-agent/newman-agent-1.0.jar.backup.${DATE_SUFFIX}"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	else
		sshpass -p password scp -o StrictHostKeyChecking=no ${DIR}/../../newman-agent/target/newman-agent-1.0.jar xap@$host:/home/xap/newman-agent/newman-agent-1.0.jar
#		sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "killall -9 java"'
		#sshpass -p password ssh xap@$host -C '/usr/bin/supervisord -c /home/xap/newman-agent/supervisord.conf'
	fi
done
