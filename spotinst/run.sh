#!/bin/bash

source ${HOME}/env.sh

case "${COMPONENT}" in
    "server")
        ./run-server.sh
        ;;
    "agent")
        ./run-agent.sh
        ;;
    *)
        echo "Unexpected component ${COMPONENT}"
        ;;
esac
