package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.TestHistoryItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tamirs
 * on 9/9/15.
 */
public class TestScoreUtils {

    public static String FAIL = "|";
    public static String PASS = ".";

    public static String DELIMETER = "_";

    private static final Logger logger = LoggerFactory.getLogger(NewmanResource.class);

    public static double score(String history){
        int intervalsCount = 0;
        if(history.isEmpty()){ // handle empty no history
            return 0;
        }
        List<String> LongHistory = Arrays.asList(history.split(" "));
        String prevRes = (LongHistory.get(0).equals(PASS)) ? PASS : FAIL;

        for (int i = 1; i < LongHistory.size(); i++) {
            if(! LongHistory.get(i).equals(prevRes)){
                intervalsCount++;
            }
            prevRes = LongHistory.get(i);
        }

        return intervalsCount;
    }

    public static String decodeShortHistoryString( Test test, List<TestHistoryItem> history, Test.Status currentStatus, Build curBuild ) {
        StringBuilder masterSb = new StringBuilder();
        StringBuilder branchSb = new StringBuilder();
        logger.info( "--Start return history for tests in build " + curBuild.toString() + ", test:" + test );
        updateTestStatusIndication( currentStatus, curBuild.getBranch(), masterSb, branchSb );

        for (TestHistoryItem testHistoryItem : history) {
            Test.Status localStatus = testHistoryItem.getTest().getStatus();
            if( localStatus != Test.Status.FAIL && localStatus != Test.Status.SUCCESS) {
                continue;
            }
            updateTestStatusIndication( localStatus, testHistoryItem.getJob().getBuildBranch(), masterSb, branchSb );
        }

        String result = branchSb.length() == 0 ? masterSb.toString() : branchSb.toString() + DELIMETER + masterSb;
        logger.info( "--return history string [" + result + "] for tests in build " + curBuild.toString() + ", test:" + test );
        return result;
    }

    private static void updateTestStatusIndication( Test.Status status, String branchName, StringBuilder masterSb, StringBuilder branchSb ){
        String localStatusIndication =  createTestStatusIndication( status );
        if( branchName.equals( NewmanResource.MASTER_BRANCH_NAME ) ){
            masterSb.append( localStatusIndication );
        }
        else{
            branchSb.append( localStatusIndication );
        }
    }

    private static String createTestStatusIndication( Test.Status status ){
        return status == Test.Status.FAIL ? FAIL +" " :PASS + " ";
    }
}