#!/bin/bash

PID=`ps -eaf | grep newman-agent | grep -v grep | awk '{print $2}'`
echo "current pid of newman agent is: ${PID}"
if [[ "" !=  "$PID" ]]; then
  echo "killing newman agent with pid: ${PID}"
  kill ${PID}
fi