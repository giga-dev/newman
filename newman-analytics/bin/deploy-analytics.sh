#!/bin/bash
#file should be in the following format: user@host on each line (one per line)
SERVER_FOLDER="/home/xap/newman-analytics"
SERVER_HOST_DEST="xap@xap-newman"
echo "deploying new analytics jar"
sshpass -p 'password' scp ../target/newman-analytics-1.0.jar ${SERVER_HOST_DEST}:${SERVER_FOLDER}
