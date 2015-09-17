package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.TestHistoryItem;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tamirs
 * on 9/9/15.
 */
public class TestScoreUtils {

    public static String FAIL = "|";
    public static String PASS = ".";

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

    public static String decodeShortHistoryString(List<TestHistoryItem> history, Test.Status currentStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append(currentStatus == Test.Status.FAIL ? FAIL +" " :PASS + " "); // check only fails --> need to add to begin of history current fail
        for (TestHistoryItem value : history) {
            if(value.getTest().getStatus() != Test.Status.FAIL && value.getTest().getStatus() != Test.Status.SUCCESS) continue;
            sb.append(value.getTest().getStatus() == Test.Status.FAIL ? FAIL +" " :PASS + " ");
        }
        return sb.toString();
    }



}
