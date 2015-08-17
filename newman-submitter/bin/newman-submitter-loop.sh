#!/bin/bash
while true; do
  # load env
  source submitter-env.sh
  DAILY_MODE=true
  H=$(date +%H)
  if (( 3 <= 10#$H && 10#$H < 19 )); then 
    echo "running in daily mode, will trigger new jobs only if changes in build were made"
    DAILY_MODE=true
  else
    echo "running in nightly mode, will trigger new jobs even if no changes where made"
    DAILY_MODE=false
    export NEWMAN_SUITES="${NEWMAN_SUITES},${NEWMAN_NIGHTLY_SUITES}"
  fi
  DIFF=`diff ${WEB_FOLDER}/running_build ${WEB_FOLDER}/pending_build`
  if [[ -z "$DIFF" && "$DAILY_MODE" = "true" ]]
  then 
    echo "no new build was created, waiting for changes..."
    sleep 30
    continue
  fi
  branch_list=`cat ${BRANCH_FILE_PATH}`
  IFS=',' read -a branch_array <<< "${branch_list}"
  for branch in "${branch_array[@]}"
  do
    echo "Clearing running build folder"
    rm -rf ${WEB_FOLDER}/running_build/*
    echo "Copying pending build to running build"
    cp -R ${WEB_FOLDER}/pending_build/* ${WEB_FOLDER}/running_build/
    echo "Waiting for webserver to sync with changes"
    sleep 60
    LOCAL_BUILDS_DIR=/home/xap/testing-grid/local-builds
    CUR_BRANCH_DIR=${LOCAL_BUILDS_DIR}/${branch}
    BUILD=`./select_build.sh ${CUR_BRANCH_DIR}`
    GS_BUILD_ZIP=$(find ${WEB_FOLDER}/running_build -name '*giga*.zip')
    GS_BUILD_ZIP=`basename ${GS_BUILD_ZIP}`
    export NEWMAN_BUILD_NUMBER=${BUILD}
    export NEWMAN_BUILD_BRANCH=${branch}
    export NEWMAN_BUILD_TESTS_METADATA=${BASE_WEB_URI}/running_build/tgrid-tests-metadata.json,${BASE_WEB_URI}/running_build/sgtest-tests.json,${BASE_WEB_URI}/running_build/http-session-tests.json,${BASE_WEB_URI}/running_build/mongodb-tests.json
    export NEWMAN_BUILD_SHAS_FILE=${BASE_WEB_URI}/running_build/metadata.txt
    export NEWMAN_BUILD_RESOURCES=${BASE_WEB_URI}/running_build/testsuite-1.5.zip,${BASE_WEB_URI}/running_build/${GS_BUILD_ZIP},${BASE_WEB_URI}/running_build/newman-artifacts.zip
    echo "NEWMAN_BUILD_NUMBER=${NEWMAN_BUILD_NUMBER}"
    echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
    echo "NEWMAN_BUILD_TESTS_METADATA=${NEWMAN_BUILD_TESTS_METADATA}"
    echo "NEWMAN_BUILD_SHAS_FILE=${NEWMAN_BUILD_SHAS_FILE}"
    echo "NEWMAN_BUILD_RESOURCES=${NEWMAN_BUILD_RESOURCES}"
    echo "Running newman submitter"
    java -jar newman-submitter-1.0.jar
  done
done

