#!/bin/bash
set -x
DIRNAME=$(dirname ${BASH_SOURCE[0]})

cd ${DIRNAME}/../newman-server/

#echo "running elm make"
#./elm-make.sh

echo "running mongo-restart.sh"
./mongo-restart.sh

echo "Running newman server"
cd bin
./newman-server.sh