#!/bin/bash
set -e
DIRNAME=$(dirname ${BASH_SOURCE[0]})

cd ${DIRNAME}/../

#echo "running elm make"
#./elm-make.sh

echo "Running mvn package "
cd ${DIRNAME}/../../
mvn clean install