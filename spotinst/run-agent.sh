#!/bin/bash
set -x
DIRNAME=$(dirname ${BASH_SOURCE[0]})

sudo ln -s ${HOME}/jdk1.8.0_45 /opt/jdk1.8.0_45


cd ${DIRNAME}/../newman-agent/bin
./newman-agent.sh