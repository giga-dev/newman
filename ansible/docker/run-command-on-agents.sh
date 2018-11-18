#!/usr/bin/env bash
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]
  then
    echo "Usage: ./run-command-on-agents.sh <SHELL_COMMAND> <FIRST_PASSWORD> <SECOND_PASSWORD>"
    exit 1
fi
export module=agents
export cmdToDo=$1
./run.sh $2 $3