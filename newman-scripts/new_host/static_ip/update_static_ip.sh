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
    newip=${array[1]}
    echo "Changing ${host} to ${newip}"

	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /etc/sysconfig/network-scripts/ifcfg-enp0s3 /home/xap/etc_sysconfig_network.$(date +\"%Y%m%d_%H%M%S\")"
	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /etc/sysconfig/network /home/xap/etc_sysconfig_network-scripts_ifcfg-enp0s3.$(date +\"%Y%m%d_%H%M%S\")"
	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C "cp /etc/resolv.conf /home/xap/etc_resolv.conf.$(date +\"%Y%m%d_%H%M%S\")"
	success=$?
	echo "ok? $?"
	if [ "$success" != "0" ]; then
		echo "Failed on host $host"
	else
		cp ${DIR}/etc_sysconfig_network-scripts_ifcfg-enp0s3 ${DIR}/etc_sysconfig_network-scripts_ifcfg-enp0s3.work
		echo "IPADDR=$newip" >> ${DIR}/etc_sysconfig_network-scripts_ifcfg-enp0s3.work
		sshpass -p password scp -o StrictHostKeyChecking=no ${DIR}/etc_sysconfig_network xap@$host:/home/xap/etc_sysconfig_network
		sshpass -p password scp -o StrictHostKeyChecking=no ${DIR}/etc_sysconfig_network-scripts_ifcfg-enp0s3.work xap@$host:/home/xap/etc_sysconfig_network-scripts_ifcfg-enp0s3
		sshpass -p password scp -o StrictHostKeyChecking=no ${DIR}/etc_resolv.conf xap@$host:/home/xap/etc_resolv.conf
		sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "mv /home/xap/etc_sysconfig_network /etc/sysconfig/network ; mv /home/xap/etc_sysconfig_network-scripts_ifcfg-enp0s3 /etc/sysconfig/network-scripts/ifcfg-enp0s3 ; mv /home/xap/etc_resolv.conf /etc/resolv.conf ; reboot"'
		rm -rf ${DIR}/etc_sysconfig_network-scripts_ifcfg-enp0s3.work
	fi

done

 for i in $(seq 0 $END); do 
     line=${lines[$i]}
 	array=(${line})
     host=${array[0]}
     echo "Rebooting ${host}"

 	sshpass -p password ssh -o StrictHostKeyChecking=no xap@$host -C 'sudo bash -c "reboot"'
	
 done
