#!/usr/bin/env bash

MODE="-d"
if [[ -n "$1" ]]; then
        if [[ "$1" == "-iii" ]]; then
                MODE=""
        else
                MODE="$1"
        fi
fi

sudo docker kill mongo-server
sudo docker rm mongo-server
sudo docker run --hostname=mongo-server --name mongo-server -p 27017:27017 -v `pwd`/data/db:/data/db --name mongo-server ${MODE} mongo:4.0.10 --nounixsocket
