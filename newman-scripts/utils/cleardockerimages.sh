#!/bin/bash

function cleardocker {
	trap "" HUP
	local supervisorpid=$(ps aux | grep supervisor | grep -v grep | awk '{ print $2 }')
	echo Killing supervisor [$supervisorpid]
	kill -9 $supervisorpid
	echo Killing Java processes
	killall -9 java

	echo Removing docker containers
	docker rm -f $(docker ps -aq)

	echo Deleting docker images
	docker rmi -f $(docker images -aq)

        echo Rebooting
	sudo reboot
}

sshpass -p password ssh -o StrictHostKeyChecking=no xap@$1 "$(typeset -f); cleardocker $1 > /tmp/cleardocker.out 2>&1 &"
