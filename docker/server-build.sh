#!/bin/bash
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`

M2=$HOME/.m2
if [[ ! -e "${M2}" ]]; then
    mkdir $HOME/.m2
fi
docker run -v ${M2}:${M2} -v "${DIRNAME}/..":"/newman" --user $(id -u) newman "/newman/newman-server/bin/build.sh"
docker pull mongo