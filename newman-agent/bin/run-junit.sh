#!/bin/sh

TEST_NAME=$1
#CP=$2

java -cp ../tests/QA/JSpacesTestSuite.jar:../tests/QA/lib/*.jar org.junit.runner.JUnitCore ${TEST_NAME}