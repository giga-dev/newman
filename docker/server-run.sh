#!/bin/bash
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`
echo $DIRNAME
MODE="-d"
if [[ -n "$1" ]]; then
        if [[ "$1" == "-iii" ]]; then
                MODE=""
        else
                MODE="$1"
        fi
fi

docker stop newman-server
docker rm newman-server

cmd="docker ps -f name=mongo-server -q"

while [[ -z "$(${cmd})" ]]; do
    echo "Waiting for mongo-server docker"
    sleep 5s
done

docker run ${MODE} --link mongo-server --rm -v "${DIRNAME}/..":"/newman" --name newman-server --user $(id -u) -p 8443:8443 newman "/newman/newman-server/bin/newman-server.sh"
