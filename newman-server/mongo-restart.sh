#!/usr/bin/env bash

docker kill rest-mongo
docker rm rest-mongo
docker run -p 27017:27017 -v `pwd`/data/db:/data/db --name rest-mongo -d mongo
