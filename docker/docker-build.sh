#!/bin/bash
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`

docker build --build-arg user=$USER --build-arg uid=$(id -u) --build-arg gid=$(id -g) -t newman ${DIRNAME}