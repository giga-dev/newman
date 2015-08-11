#!/bin/bash
SUBMITTER_FOLDER="/home/xap/testing-grid/bin"
HOST_ADDRESS="xap@192.168.50.63"
echo "killing old submitter"
sshpass -p 'password' ssh ${HOST_ADDRESS} 'pkill java'
echo "deploying new submitter jar"
sshpass -p 'password' scp ../target/newman-submitter-1.0.jar ${HOST_ADDRESS}:${SUBMITTER_FOLDER}
echo "starting new submitter"
sshpass -p 'password' ssh ${HOST_ADDRESS} 'cd /home/xap/testing-grid/bin && ./nohup-newman-submitter-loop.sh'

