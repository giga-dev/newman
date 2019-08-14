#!/usr/bin/env bash

export SUITE_ID=5a9bef64b385946dd9dd1111
export BUILD_ID=5c06de81b3859448094e6791
export CONFIG_ID=5bf160bb1f31eb789fc0fa65 # put you config id here

export SUITE_TYPE=sgtest
export SGTEST_DIR=~/sgtest

java -Dnewman.agent.server-host=groot.gspaces.com -jar ../target/newman-agent-1.0.jar setup ${SUITE_ID} ${BUILD_ID} ${CONFIG_ID}
