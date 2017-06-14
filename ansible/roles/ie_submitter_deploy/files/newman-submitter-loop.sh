#!/bin/bash
while true; do
        source submitter-env.sh

        # take branches from file
        branch_list=`cat ${BRANCH_FILE_PATH}`
        IFS=',' read -a branch_array <<< "${branch_list}"
        echo "starting loop over branches ..."

        # loop over all branches
        for branch in "${branch_array[@]}"
        do
                echo "Start submitting IE jobs. Date is [`date`]"
                export NEWMAN_BUILD_TAGS="INSIGHTEDGE"
                export NEWMAN_BUILD_BRANCH=${branch}
                echo "NEWMAN_SUITES=${NEWMAN_SUITES}"
                echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
                echo "NEWMAN_BUILD_TAGS=${NEWMAN_BUILD_TAGS}"
                echo "NEWMAN_MODE=${NEWMAN_MODE}"
                #checking future job
                java -jar newman-submitter-1.0.jar
                HAS_FUTURE_JOBS=$?
                echo "Finished submitting IE jobs. HAS_FUTURE_JOBS? [$HAS_FUTURE_JOBS]. Date is [`date`] "
                echo "Bye bye..."
#                while [ $HAS_FUTURE_JOBS -ne 0 ]; do
#                        echo "Has future jobs, trying again..."
#                        java -jar newman-submitter-1.0.jar
#                        HAS_FUTURE_JOBS=$?
#                        sleep 120
#                done
#                echo "finish submitter work!"
        done
done
