#!/usr/bin/env bash
#export BRANCH_FILE_PATH="/data/newman/testing-grid/bin/branch_list.txt"
#export NEWMAN_HOST=192.168.80.1
#export NEWMAN_PORT=8443
#export NEWMAN_USER_NAME=root
#export NEWMAN_PASSWORD=root
#export NEWMAN_SUITES_FILE_LOCATION="/data/newman/testing-grid/bin/submitter-env.ini"

export NEWMAN_HOST=192.168.80.1
export NEWMAN_PORT=8443
export NEWMAN_USER_NAME=root
export NEWMAN_PASSWORD=root
export NEWMAN_BUILD_BRANCH=Rami-Playground
export NEWMAN_BUILD_NUMBER=17.1.2-Rami-Playground-ci-2730
export NEWMAN_BUILD_TAGS=
export NEWMAN_BUILD_SHAS_FILE=https://gs-releases-us-east-1.s3.amazonaws.com/test-build-newman/17.1.2/Rami-Playground/17.1.2-Rami-Playground-ci-2730/metadata.txt

java -cp ./newman-submitter-2.0.jar com.gigaspaces.newman.NewmanBuildSubmitter