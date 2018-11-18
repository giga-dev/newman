#!/usr/bin/env bash
JAVA_URL="http://hercules/javas/jdk-8u45-linux-x64.tar.gz"
JAVA_FILENAME="jdk-8u45-linux-x64.tar.gz"
JAVA_FOLDERNAME="jdk8u45"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

if [ -z "$1" ]
  then
    echo "usage: run_command.sh COMMAND_TO_RUN"
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

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$@"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	fi
done
