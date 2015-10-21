package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.FutureJob;
import com.gigaspaces.newman.beans.JobRequest;
import com.gigaspaces.newman.beans.Suite;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

/**
 * Created by tamirs
 * on 10/21/15.
 */
public class NewmanFutureJobSubmitter {

    private static final String HOST = "xap-newman"; // NOTE - can be 'localhost' for local running
    private static final String PRT = "8443";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    // NOTE - change here info!
    private static final String BUILD_ID = ""; // for example: 56277de629f67f791db25554
    private static final String SUITE_ID = ""; // for example: 55b0affe29f67f34809c6c7b
    private static final String YOUR_NAME = ""; // for example: tamirs


    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException {
        NewmanClient newmanClient = NewmanClient.create(HOST, PRT, USERNAME, PASSWORD);

        valid(newmanClient);
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(BUILD_ID);
        jobRequest.setSuiteId(SUITE_ID);
        FutureJob futureJob = newmanClient.createFutureJob(jobRequest, YOUR_NAME).toCompletableFuture().get();
        System.out.println(futureJob.getAuthor() + " create future job with id: " + futureJob.getId() + ", build id: " + futureJob.getBuildID() + ", and suite id: " + futureJob.getSuiteID() + ".");
    }


    private static void valid(NewmanClient newmanClient){
        if(newmanClient == null){
            throw new RuntimeException("newmanClient did not connect");
        }
        if(BUILD_ID==null || BUILD_ID.isEmpty()){
            throw new RuntimeException("BUILD_ID is empty string or null");
        }
        if(SUITE_ID == null || SUITE_ID.isEmpty()){
            throw new RuntimeException("SUITE_ID is empty string or null");
        }
        try{
            newmanClient.getBuild(BUILD_ID).toCompletableFuture().get();
        }catch (Exception e){
            throw new RuntimeException("BUILD_ID: "+ BUILD_ID +" does not exist");
        }
        try{
            newmanClient.getSuite(SUITE_ID).toCompletableFuture().get();
        }catch (Exception e){
            throw new RuntimeException("SUITE_ID: "+ SUITE_ID +" does not exist");
        }
        if(YOUR_NAME == null || YOUR_NAME.isEmpty()){
            throw new RuntimeException("YOUR_NAME is empty string or null");
        }


    }

}
