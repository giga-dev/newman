#!/usr/bin/env bash

export SUITE_ID=55b0affe29f67f34809c6c7b
export BUILD_ID=5798bfae29f67f02db5d290a

export SUITE_TYPE=sgtest
export SGTEST_DIR=~/sgtest

java -Dnewman.agent.server-host=xap-newman -jar ../target/newman-agent-1.0.jar setup ${SUITE_ID} ${BUILD_ID}