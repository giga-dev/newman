#!/bin/bash
#file should be in the following format: user@host on each line (one per line)
SERVER_FOLDER="/home/xap/newman-server"
SERVER_HOST_DEST="xap@xap-newman"
echo "killing old server"
sshpass -p 'password' ssh ${SERVER_HOST_DEST} 'pkill -9 java'
echo "deploying new server jar"
sshpass -p 'password' scp ../target/newman-server-1.0.jar ${SERVER_HOST_DEST}:${SERVER_FOLDER}
echo "starting new server"
sshpass -p 'password' ssh ${SERVER_HOST_DEST} 'cd /home/xap/newman-server && ./run.sh'

