#!/bin/bash
set -e
DIRNAME=$(dirname ${BASH_SOURCE[0]})

echo "Running mvn package "
cd ${DIRNAME}/../../
mvn clean install