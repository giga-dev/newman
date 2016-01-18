#!/bin/bash
export BRANCH_FILE_PATH="/home/xap/docker/branch_list.txt"
export NEWMAN_HOST=xap-newman
export NEWMAN_PORT=8443
export NEWMAN_USER_NAME=root
export NEWMAN_PASSWORD=root
#SUITES
export XAP_CORE=55acfec729f67f34809b62d8
export SERVICE_GRID=55b0affe29f67f34809c6c7b
export WEB_UI=55effbf329f67f0322d26c0e
export SG_SSH=55c7274d29f67f6c2fdf0c98

export ROCKS_DB=569cef5e29f67f2c7ae17138
export MAP_DB=55c74da229f67f6c2fdf0c99
export ROCKS_DB=561f602729f67f791dafe053
export MAP_DB=569b879829f67f2c7ae0c312
export OFF_HEAP=55c707d129f67f6c2fdefe48
export DISCONNECT=55e5632d29f67f1d1f094198
export ESM_SECURITY=55db294729f67f08c6d8ee8d
export WAN=55d5a08829f67f08c6d81a88
export MONGO_DB=55d082f529f67f1ce89d8e40
export HTTP_SESSION=55c9bb8329f67f1020d4ac02
export ESM=55b8b8f629f67f1d0f223f5b
export SECURITY=55cb394629f67f1ce89d15ab
export DOTNET=564045b529f67f3a0fd7c1d7
export JETTY9=55c9e99d29f67f1020d4ba6d

export SSD=561bc78b29f67f4d80f6076e
#SUITES TO RUN
export NEWMAN_SUITES=${XAP_CORE},${SERVICE_GRID},${SG_SSH}
export NEWMAN_NIGHTLY_SUITES=${ROCKS_DB},${MAP_DB},${OFF_HEAP},${DISCONNECT},${ESM_SECURITY},${WAN},${MONGO_DB},${HTTP_SESSION},${ESM},${SECURITY},${DOTNET},${JETTY9},${WEB_UI}