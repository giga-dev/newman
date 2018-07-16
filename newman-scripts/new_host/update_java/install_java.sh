#!/bin/bash
JAVA_URL="http://hercules/javas/<new_java_version>.tar.gz"
JAVA_FILENAME="<new_java_version>.tar.gz"
JAVA_FOLDERNAME="<new_java_version_short>"

#JAVA_URL="http://hercules/javas/jdk-8u45-linux-x64.tar.gz"
#JAVA_FILENAME="jdk-8u45-linux-x64.tar.gz"
#JAVA_FOLDERNAME="jdk8u45"

#JAVA_URL="http://hercules/javas/jdk-9.0.4_linux-x64_bin.tar.gz"
#JAVA_FILENAME="jdk-9.0.4_linux-x64_bin.tar.gz"
#JAVA_FOLDERNAME="jdk904"

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

	# sshpass -p password ssh xap@$host -C "mkdir -p ~/software/java && cd ~/software/java && ln -s /opt/jdk1.8.0_144 current"

#  sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "mkdir -p ~/software/java && cd ~/software/java && rm -rf temp ${JAVA_FOLDERNAME} && mkdir temp && rm -rf ${JAVA_FILENAME}* && wget -q ${JAVA_URL} && tar -zxf ${JAVA_FILENAME} -C temp && mv temp/* temp/${JAVA_FOLDERNAME} && mv temp/${JAVA_FOLDERNAME} . && rm -rf temp"
#sshpass -p password ssh -o StrictHostKeyChecking=no  xap@$host -C "cd /opt ; sudo ln -nfs /home/xap/software/java/${JAVA_FOLDERNAME} <JDK_name>"

#  sshpass -p password ssh -o StrictHostKeyChecking=no  xap@$host -C "cd /opt && sudo ln -nfs /home/xap/software/java/jdk904 jdk-9.0.4"



  sshpass -p password ssh -o StrictHostKeyChecking=no  xap@$host -C "cd /opt && ls"


# sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cd /home/xap/software/java && sudo ln -nfs /home/xap/software/java/jdk8u45 current"

#	sshpass -p password ssh xap@$host -C "sudo sed -i \"s/export JAVA_HOME=.*/export JAVA_HOME=\/home\/xap\/software\/java\/current/\" /etc/bashrc && sudo reboot"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	fi
done