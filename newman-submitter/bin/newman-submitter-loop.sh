#!/bin/bash
while true; do
    #load env
    source submitter-env.sh
    HOURS=$(date +%H)
    # determine if daily mode
    # nightly greater/equal then 19 or less/eqal then 23
    if [ $HOURS -ge 19 -a $HOURS -le 22 ]; then
        echo "running in nightly mode, will trigger new jobs even if no changes where made, date is `date`"
        export NEWMAN_SUITES="${NEWMAN_SUITES},${NEWMAN_NIGHTLY_SUITES}"
        export NEWMAN_MODE="NIGHTLY"
        export NEWMAN_BUILD_TAGS="DOTNET"
    else
        echo "running in daily mode, will trigger new jobs only if changes in build were made, date is `date`"
        export NEWMAN_MODE="DAILY"
    fi
    # take branches from file
    branch_list=`cat ${BRANCH_FILE_PATH}`
    IFS=',' read -a branch_array <<< "${branch_list}"
    echo "starting loop over branches ....."
    # loop over all branches
    for branch in "${branch_array[@]}"
    do
        export NEWMAN_BUILD_BRANCH=${branch}
        echo "NEWMAN_SUITES=${NEWMAN_SUITES}"
        echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
        echo "NEWMAN_BUILD_TAGS=${NEWMAN_BUILD_TAGS}"
        echo "NEWMAN_MODE=${NEWMAN_MODE}"
        #checking future job
        java -jar newman-submitter-1.0.jar
        HAS_FUTURE_JOBS=$?
        echo "HAS_FUTURE_JOBS=${HAS_FUTURE_JOBS}"
        while [ $HAS_FUTURE_JOBS -ne 0 ]; do
            echo "Has future jobs, trying again..."
            java -jar newman-submitter-1.0.jar
            HAS_FUTURE_JOBS=$?
            sleep 120
        done
        echo "finish submitter work!"
    done
done
