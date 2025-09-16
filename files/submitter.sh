#!/usr/bin/env bash
source submitter-env.sh

# take branches from file
branch_list=`cat ${BRANCH_FILE_PATH}`
IFS=',' read -a branch_array <<< "${branch_list}"
echo "starting loop over branches ..."

# loop over all branches
for branch in "${branch_array[@]}"
do
        echo "Start submitting XAP jobs. Date is [`date`]"
        # current hour
        HOURS=$(date +%H)

        # check if nightly or daily mode - every branch
#                if [ $HOURS -ge 20 -a $HOURS -le 23 ]; then
        if [ $HOURS -ge 20 -a $HOURS -le 23 ]; then
                echo "running in nightly mode, will trigger new jobs even if no changes where made, date is `date`"
                export NEWMAN_MODE="NIGHTLY"
                export NEWMAN_BUILD_TAGS="XAP,DOTNET"
        else
                echo "running in daily mode, will trigger new jobs only if changes in build were made, date is `date`"
                export NEWMAN_MODE="DAILY"
                export NEWMAN_BUILD_TAGS="XAP"
        fi

        export NEWMAN_BUILD_BRANCH=${branch}

        echo "NEWMAN_BUILD_BRANCH=${NEWMAN_BUILD_BRANCH}"
        echo "NEWMAN_BUILD_TAGS=${NEWMAN_BUILD_TAGS}"
        echo "NEWMAN_MODE=${NEWMAN_MODE}"

        #checking future job
        java -jar newman-submitter-2.0.jar
        HAS_FUTURE_JOBS=$?
        echo "FINISHED SUBMIT LOOP!! [HAS_FUTURE_JOBS=${HAS_FUTURE_JOBS}]. Date is [`date`]"
done
