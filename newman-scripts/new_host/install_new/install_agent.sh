#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOSTS_FILE="${DIR}/../../newman-hosts"
if [ ! -e "${HOSTS_FILE}" ]; then
	echo "No such file ${HOSTS_FILE}"
	exit 1
fi


function install_newman {
    local NEWMAN_AGENT_GROUPNAME=$1
    local NEWMAN_SERVER_HOST=$2
    mkdir ~/automation
    cd ~/automation

    sudo yum install git -y

    git clone https://github.com/giga-dev/newman.git
    cd newman/docker
    `pwd`/docker-build.sh
    `pwd`/agent-build.sh
    cd ~/automation/newman/docker/

     local envFile=../newman-agent/bin/env.sh
     echo "export NEWMAN_SERVER_HOST=${NEWMAN_SERVER_HOST}" > ${envFile}
     echo "export NEWMAN_AGENT_GROUPNAME=\"${NEWMAN_AGENT_GROUPNAME}\"" >> ${envFile}
#     echo "export NEWMAN_AGENT_CAPABILITIES=\"DOCKER,LINUX,MVN,PMEM\"" >> ${envFile}

    sudo bash -c 'echo "export JAVA_HOME=/home/xap/software/java/current" >> /etc/profile'
    sudo bash -c 'echo "export PATH=\${JAVA_HOME}/bin:\${PATH}" >> /etc/profile'
    sudo bash -c 'echo "export M2_HOME=/opt/apache-maven-3.3.9" >> /etc/profile'
    sudo bash -c 'echo "PATH=/opt/apache-maven-3.3.9/bin:\$PATH" >> /etc/profile'



     supervisorctl reread
     supervisorctl reload
}

function copy_files {
    local host=$1
    sshpass -p password scp ${DIR}/supervisor_newman.conf root@${host}:/etc/supervisord.d/
    success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
		exit 1
	fi
}


readarray lines < ${HOSTS_FILE}

END=$((${#lines[@]} - 1))

for i in $(seq 0 $END); do 
	line=${lines[$i]}

    array=(${line})
    host=${array[0]}
	echo $host


#	copy_files $host

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "$(declare -f install_newman) ; install_newman 'imc-srv01' '18.224.235.227'"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
		exit 1
	fi

done