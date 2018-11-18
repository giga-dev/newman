#!/usr/bin/env bash
export BRANCH_FILE_PATH="/home/xap/insightedge/branch_list.txt"
export NEWMAN_HOST=xap-newman
export NEWMAN_PORT=8443
export NEWMAN_USER_NAME=root
export NEWMAN_PASSWORD=root
#SUITES
export INSIGHTEDGE_PREMIUM=59f25af6b3859424cac590b0
export INSIGHTEDGE_COMMUNITY=59f25af6b3859424cac590af
#SUITES TO RUN
export NEWMAN_SUITES=${INSIGHTEDGE_PREMIUM},${INSIGHTEDGE_COMMUNITY}