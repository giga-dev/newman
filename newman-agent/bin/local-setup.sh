#!/usr/bin/env bash

export SUITE_ID=63aabab94cedfd000b15ce12
export BUILD_ID=63aaddaa4cedfd000b15ce41
export CONFIG_ID=5b4c9342b3859411ee82c265 # put you config id here
export STORAGE_SERVER=s3-eu-west-1.amazonaws.com/xap-test/test-build-newman
export SUITE_TYPE=sgtest
export SGTEST_DIR=~/sgtest

java -Dnewman.agent.server-host=groot.gspaces.com -jar ../target/newman-agent-1.0.jar setup ${SUITE_ID} ${BUILD_ID} ${CONFIG_ID}
