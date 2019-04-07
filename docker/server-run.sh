#!/bin/bash
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`

MODE="-d"
if [[ -n "$1" ]]; then
        if [[ "$1" == "-iii" ]]; then
                MODE=""
        else
                MODE="$1"
        fi
fi


docker run ${MODE} --link mongo-server -v "${DIRNAME}/..":"/newman" --name newman-server --user $(id -u) -p 8443:8443 newman "/newman/newman-server/bin/newman-server.sh"