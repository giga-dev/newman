package com.gigaspaces.newman.report.daily;

import com.gigaspaces.newman.NewmanClient;
import com.gigaspaces.newman.NewmanReporterConfig;
import com.gigaspaces.newman.beans.*;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by moran on 8/13/15.
 */
public class DailyReport implements org.quartz.Job{

    private static final Logger logger = LoggerFactory.getLogger(DailyReport.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        //Access shared job data
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        AtomicReference<Build> buildRef = (AtomicReference<Build>) jobDataMap.get("mutable.ref.build");
        NewmanReporterConfig config = (NewmanReporterConfig) jobDataMap.get("immutable.newman.config");

        NewmanClient newmanClient = null;
        try {
            newmanClient = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                    config.getNewmanServerRestUser(), config.getNewmanServerRestPassword());

            prepareReport(buildRef, newmanClient);


        } catch (Exception e) {
            logger.warn("Failed to execute daily report", e);
        } finally {
        if (newmanClient != null) {
            newmanClient.close();
        }
    }
    }

    private void prepareReport(AtomicReference<Build> buildRef, NewmanClient newmanClient) throws Exception {

        DashboardData dashboardData = newmanClient.getDashboard().toCompletableFuture().get();

        List<Build> historyBuilds = dashboardData.getHistoryBuilds();
        if (historyBuilds.size() == 0) {
            logger.info("No history builds to generate report");
            return;
        }

        Build latest_build = historyBuilds.get(0);
        Build previous_build = getPreviousBuild(historyBuilds, buildRef.get(), latest_build);

        StringBuilder subject = prepareSubject(latest_build);
        StringBuilder body = prepareBody(newmanClient, latest_build, previous_build);

        System.out.println(subject);
        System.out.println("^^^^^^^^^^");
        System.out.println(body);
        System.out.println("---\n");

        //save latest_build for next time we wake up
        buildRef.set(latest_build);
    }

    private StringBuilder prepareBody(NewmanClient newmanClient, Build latest_build, Build previous_build) throws ExecutionException, InterruptedException {

        StringBuilder suiteComparison = prepareSuiteComparison(newmanClient, latest_build, previous_build);

        StringBuilder body = new StringBuilder();
        body.append(suiteComparison).append("\n");
        body.append("latest build: ").append(latest_build.getName()).append(" ").append(latest_build.getId()).append("\n");
        body.append("previous build: ").append(previous_build.getName()).append(" ").append(previous_build.getId());

        return body;
    }

    private StringBuilder prepareSuiteComparison(NewmanClient newmanClient, Build latest_build, Build previous_build) throws ExecutionException, InterruptedException {
        StringBuilder suiteData = new StringBuilder();

        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latest_build.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previous_build.getId());
        Set<SuiteDiff> suiteDiffMap = compare(latest_mapSuite2Job, previous_mapSuite2Job);

        int positiveDiff = 0;
        int negativeDiff = 0;
        for (SuiteDiff diff : suiteDiffMap) {
            if (diff.diffFailedTests > 0) {
                positiveDiff += diff.diffFailedTests;
            } else if (diff.diffFailedTests < 0) {
                negativeDiff += diff.diffFailedTests;
            }
        }

        if (positiveDiff != 0 || negativeDiff != 0) {
            suiteData.append("diff ");
            if (positiveDiff != 0) {
                suiteData.append('(').append('+').append(positiveDiff).append('↑').append(')');
            }
            if (negativeDiff != 0) {
                suiteData.append('(').append(negativeDiff).append('↓').append(')');
            }
            suiteData.append("\n");
        } else {
            suiteData.append("No diff").append("\n");
        }

        for (SuiteDiff diff : suiteDiffMap) {
            suiteData.append(diff.suite.getName()).append("\t");
            suiteData.append(diff.failedTests);
            if (diff.diffFailedTests != 0) {
                suiteData.append(" ").append('(');
                if (diff.diffFailedTests > 0) {
                    suiteData.append('+').append(diff.diffFailedTests).append('↑');
                } else {
                    suiteData.append(diff.diffFailedTests).append('↓');
                }
                suiteData.append(')');
            }
            suiteData.append("\t\t");
            suiteData.append(diff.totalTests);
            if (diff.diffTotalTests != 0) {
                suiteData.append(" ").append('(');
                if (diff.diffTotalTests > 0) {
                    suiteData.append('+').append(diff.diffTotalTests).append('↑');
                } else {
                    suiteData.append(diff.diffTotalTests).append('↓');
                }
                suiteData.append(')');
            }
            suiteData.append("\n");
        }

        return suiteData;
    }

    private StringBuilder prepareSubject(Build latest_build) {
        StringBuilder subject = new StringBuilder();
        subject.append('(').append(latest_build.getBranch()).append(')').append(" build ").append(latest_build.getName())
                .append(" with ").append(latest_build.getBuildStatus().getFailedTests()).append(" failures");
        return subject;
    }

    private Set<SuiteDiff> compare(Map<String, Job> latest_mapSuite2Job, Map<String, Job> previous_mapSuite2Job) {
        Set<SuiteDiff> set = new TreeSet<SuiteDiff>();
        for (Map.Entry<String, Job> entry : latest_mapSuite2Job.entrySet()) {
            String suiteId = entry.getKey();
            Job latestJob = entry.getValue();
            Job previousJob = previous_mapSuite2Job.get(suiteId);
            set.add(createSuiteDiff(latestJob, previousJob));
        }

        return set;
    }

    private SuiteDiff createSuiteDiff(Job latestJob, Job previousJob) {
        SuiteDiff suiteDiff = new SuiteDiff();
        suiteDiff.suite = latestJob.getSuite();
        suiteDiff.failedTests = latestJob.getFailedTests();
        suiteDiff.totalTests  = latestJob.getTotalTests();

        if (previousJob != null) {
            suiteDiff.diffFailedTests = latestJob.getFailedTests() - previousJob.getFailedTests();
            suiteDiff.diffTotalTests = latestJob.getTotalTests() - previousJob.getTotalTests();
        }

        return suiteDiff;
    }

    private Build getPreviousBuild(List<Build> historyBuilds, Build previous_build, Build latest_build) {
        if (previous_build != null) {
            return previous_build;
        } else if (historyBuilds.size() > 1) {
            return historyBuilds.get(1);
        } else {
            return latest_build;
        }
    }

    /**
     * @return a map of suiteId to job corresponding to the buildId
     */

    private Map<String, Job> getJobsByBuildId(NewmanClient newmanClient, String buildId) throws ExecutionException, InterruptedException {
        Map<String, Job> map = new HashMap<String, Job>();
        Batch<Job> jobBatch = newmanClient.getJobs(buildId).toCompletableFuture().get();
        for (Job job : jobBatch.getValues()) {
            map.put(job.getSuite().getId(), job);
        }

        return map;
    }

    private class SuiteDiff implements Comparable<SuiteDiff> {
        Suite suite;
        int failedTests;
        int diffFailedTests;
        int totalTests;
        int diffTotalTests;

        @Override
        public int compareTo(SuiteDiff o) {
            double thisFailures = ((double)failedTests)/totalTests;
            double otherFailures = ((double)o.failedTests)/o.totalTests;
            int compareFailures = Double.valueOf(thisFailures).compareTo(Double.valueOf(otherFailures));
            if (compareFailures != 0) {
                return -1 * compareFailures;
            }

            //if equal go for diff
            int compareDiff = Integer.valueOf(diffFailedTests).compareTo(Integer.valueOf(o.diffFailedTests));
            if (compareDiff != 0) {
                return -1 * compareDiff;
            }

            //never return 0 when using TreeSet
            return -1;
        }
    }
}
