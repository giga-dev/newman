#!/bin/bash
ps -ef | grep newman-agent | grep -v grep | awk '{print $2}' | xargs kill -9
sleep 5
pkill -9 java
docker rm -f $(docker ps -a -q)