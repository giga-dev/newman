#!/usr/bin/env bash

export SUITE_ID=59f25af7b3859424cac590b5
export BUILD_ID=5b03e626b385946642ec861f

export SUITE_TYPE=sgtest
export SGTEST_DIR=~/sgtest

java -Dnewman.agent.server-host=xap-newman -jar ../target/newman-agent-1.0.jar setup ${SUITE_ID} ${BUILD_ID}