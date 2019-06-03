#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi

#value=`cat ${HOSTS_FILE}`

function install_supervisor {
    sudo yum install python-pip -y
	sudo pip install supervisor
	sudo mkdir -p /etc/supervisord.d

    sudo groupadd supervisor
    sudo usermod -a xap -G supervisor

}

function copy_files {
    local host=$1
    sshpass -p password scp ${DIR}/supervisord.conf root@${host}:/etc/supervisord.conf
    sshpass -p password scp ${DIR}/supervisord.service root@${host}:/usr/lib/systemd/system/supervisord.service
}

function start_supervisor {
    sudo systemctl start supervisord
	sudo systemctl enable supervisord
}

readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

for i in $(seq 0 $END); do 
	line=${lines[$i]}

    array=(${line})
    host=${array[0]}
	echo $host

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$(declare -f install_supervisor) ; install_supervisor"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
		exit 1
	fi

	copy_files $host

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$(declare -f start_supervisor) ; start_supervisor"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	fi

done