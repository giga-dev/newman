#!/usr/bin/env bash

SCRIPT_NAME=${BASH_SOURCE[0]}
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

HOSTS_FILE="$1"
COMMAND_TO_RUN="$2"

function print_usage {
    echo "Usage: ${SCRIPT_NAME} <hosts file> <single command to run>"
}

if [ -z "${SSH_PASSWORD}" ]; then
    echo "SSH_PASSWORD env var must be defined"
    exit 1
fi

if [ -z "${HOSTS_FILE}" ]; then
    echo "First argument is missing"
    print_usage
    exit 1
elif [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	print_usage
	exit 1
fi

if [ -z "${COMMAND_TO_RUN}"  ]; then
    echo "Second argument is missing."
    print_usage
    exit 1
fi

function log {
    echo ">> $@"
}
readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

# Update /etc/hosts
for i in $(seq 0 $END); do 
	line=${lines[$i]}
	
    array=(${line})
    host=${array[0]}
	log "Running on host $host"

	sshpass -p ${SSH_PASSWORD} ssh xap@$host -C "${COMMAND_TO_RUN}"
	success=$?
	if [ "$success" == "0" ]; then
	    log "Succeeded on host $host"
	else
		log "Failed on host $host ($success)"
	fi
done
