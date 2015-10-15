#!/bin/bash
while true; do
  # load env
  source submitter-env.sh
  DAILY_MODE=true
  H=$(date +%H)
  if (( 3 <= 10#$H && 10#$H < 18 )); then
    echo "running in daily mode, will trigger new jobs only if changes in build were made, date is `date`"
    DAILY_MODE=true
  else
    echo "running in nightly mode, will trigger new jobs even if no changes where made, date is `date`"
    DAILY_MODE=false
    export NEWMAN_SUITES="${NEWMAN_SUITES},${NEWMAN_NIGHTLY_SUITES}"
  fi
  branch_list=`cat ${BRANCH_FILE_PATH}`
  IFS=',' read -a branch_array <<< "${branch_list}"
  for branch in "${branch_array[@]}"
  do
    echo "checking if builder pushed a new build to pending builds under the branch: ${branch}"
    DIFF=`diff ${WEB_FOLDER}/running_build/${branch} ${WEB_FOLDER}/pending_build/${branch}`
    if [[ -z "$DIFF" && "$DAILY_MODE" = "true" ]]
    then
      echo "no new build was created, waiting for changes..."
      sleep 30
      continue
    fi
    echo "Clearing running build folder under branch: ${branch}"
    #rm -rf ${WEB_FOLDER}/running_build/*
    rm -rf ${WEB_FOLDER}/running_build/${branch}/*
    echo "Copying pending build to running build under branch: ${branch}"
    #cp -R ${WEB_FOLDER}/pending_build/* ${WEB_FOLDER}/running_build/
    cp -R ${WEB_FOLDER}/pending_build/${branch}/* ${WEB_FOLDER}/running_build/${branch}/
    echo "Waiting for webserver to sync with changes"
    sleep 60
    LOCAL_BUILDS_DIR=/home/xap/testing-grid/local-builds
    CUR_BRANCH_DIR=${LOCAL_BUILDS_DIR}/${branch}
    BUILD=`./select_build.sh ${CUR_BRANCH_DIR}`
    echo "submitting build is $BUILD date is `date` daily mode is $DAILY_MODE"
    #GS_BUILD_ZIP=$(find ${WEB_FOLDER}/running_build -name '*giga*.zip')
    GS_BUILD_ZIP=$(find ${WEB_FOLDER}/running_build/${branch} -name '*giga*.zip')
    GS_BUILD_ZIP=`basename ${GS_BUILD_ZIP}`
    export NEWMAN_BUILD_NUMBER=${BUILD}
    export NEWMAN_BUILD_BRANCH=${branch}
    #added branch to each URI
    export NEWMAN_BUILD_TESTS_METADATA=${BASE_WEB_URI}/running_build/${branch}/tgrid-tests-metadata.json,${BASE_WEB_URI}/running_build/${branch}/sgtest-tests.json,${BASE_WEB_URI}/running_build/${branch}/http-session-tests.json,${BASE_WEB_URI}/running_build/${branch}/mongodb-tests.json
    export NEWMAN_BUILD_SHAS_FILE=${BASE_WEB_URI}/running_build/${branch}/metadata.txt
    export NEWMAN_BUILD_RESOURCES=${BASE_WEB_URI}/running_build/${branch}/SGTest-sources.zip,${BASE_WEB_URI}/running_build/${branch}/testsuite-1.5.zip,${BASE_WEB_URI}/running_build/${branch}/${GS_BUILD_ZIP},${BASE_WEB_URI}/running_build/${branch}/newman-artifacts.zip
    echo "NEWMAN_BUILD_NUMBER=${NEWMAN_BUILD_NUMBER}"
    echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
    echo "NEWMAN_BUILD_TESTS_METADATA=${NEWMAN_BUILD_TESTS_METADATA}"
    echo "NEWMAN_BUILD_SHAS_FILE=${NEWMAN_BUILD_SHAS_FILE}"
    echo "NEWMAN_BUILD_RESOURCES=${NEWMAN_BUILD_RESOURCES}"
    echo "Running newman submitter"
    java -jar newman-submitter-1.0.jar
  done
done

