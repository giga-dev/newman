package com.gigaspaces.newman.report.daily;

import com.gigaspaces.newman.NewmanClient;
import com.gigaspaces.newman.NewmanReporterConfig;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.smtp.mailman.MailProperties;
import com.gigaspaces.newman.smtp.mailman.Mailman;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
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

            StringTemplate htmlTemplate = createHtmlTemplate();

            sendEmail(buildRef, newmanClient, config, htmlTemplate);


        } catch (Exception e) {
            logger.warn("Failed to execute daily report", e);
        } finally {
        if (newmanClient != null) {
            newmanClient.close();
        }
    }
    }

    private StringTemplate createHtmlTemplate() {
        StringTemplateGroup group =  new StringTemplateGroup("group",
                Paths.get(".").toAbsolutePath().normalize().toString(),
                DefaultTemplateLexer.class);

        return group.getInstanceOf("daily_report");
    }

    private void sendEmail(AtomicReference<Build> buildRef, NewmanClient newmanClient, NewmanReporterConfig config, StringTemplate htmlTemplate) throws Exception {

        DashboardData dashboardData = newmanClient.getDashboard().toCompletableFuture().get();

        List<Build> historyBuilds = dashboardData.getHistoryBuilds();
        if (historyBuilds.size() == 0) {
            logger.info("No history builds to generate report");
            return;
        }

        Build latest_build = historyBuilds.get(0);
        Build previous_build = getPreviousBuild(historyBuilds, buildRef.get(), latest_build);

        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latest_build.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previous_build.getId());
        Set<SuiteDiff> suiteDiffs = compare(latest_mapSuite2Job, previous_mapSuite2Job);
        SuiteDiffSummary summary = getDiffSummary(suiteDiffs);

        htmlTemplate.setAttribute("summary", summary);
        htmlTemplate.setAttribute("diffs", suiteDiffs);
        htmlTemplate.setAttribute("latestUrl", "https://xap-newman:8443/#!/build/" + latest_build.getId()); //TODO extract URL from newman client
        htmlTemplate.setAttribute("latestBuildName", latest_build.getName());
        htmlTemplate.setAttribute("previousUrl", "https://xap-newman:8443/#!/build/" + previous_build.getId());
        htmlTemplate.setAttribute("previousBuildName", previous_build.getName());


        String subject = prepareSubject(latest_build);
        String body = htmlTemplate.toString();

        System.out.println(subject);
        System.out.println(body);

        Mailman.createHtmlEmail(new MailProperties().setPassword(config.getNewmanMailPassword()),
                config.getNewmanMailUser(), config.getNewmanMailRecipients(), subject, body);

        //save latest_build for next time we wake up
        buildRef.set(latest_build);
    }

    private SuiteDiffSummary getDiffSummary(Set<SuiteDiff> suiteDiffs) {
        SuiteDiffSummary summary = new SuiteDiffSummary();
        for (SuiteDiff diff : suiteDiffs) {
            if (diff.diffFailedTests > 0) {
                summary.positiveDiff += diff.diffFailedTests;
            } else if (diff.diffFailedTests < 0) {
                summary.negativeDiff += diff.diffFailedTests;
            }
        }
        return summary;
    }

    private String prepareSubject(Build latest_build) {
        StringBuilder subject = new StringBuilder();
        subject.append('(').append(latest_build.getBranch()).append(')').append(" build ").append(latest_build.getName())
                .append(" with ").append(latest_build.getBuildStatus().getFailedTests()).append(" failures");
        return subject.toString();
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

    private static class SuiteDiffSummary {
        int positiveDiff;
        int negativeDiff;

        //
        // reflection methods used by org.antlr.stringtemplate (see daily_report.st files)
        //

        public int getPositiveDiff() {
            return positiveDiff;
        }

        public int getNegativeDiff() {
            return negativeDiff;
        }

        public boolean isPositiveDiff() {
            return positiveDiff > 0;
        }

        public boolean isNegativeDiff() {
            return negativeDiff > 0;
        }
    }

    private static class SuiteDiff implements Comparable<SuiteDiff> {
        Suite suite;
        int failedTests;
        int diffFailedTests;
        int totalTests;
        int diffTotalTests;

        @Override
        public int compareTo(SuiteDiff o) {
            //compare failed+diff divided by total+diff
            double thisFailures = ((double)(failedTests+diffFailedTests))/(totalTests+diffTotalTests);
            double otherFailures = ((double)(o.failedTests+o.diffFailedTests))/(o.totalTests+o.diffTotalTests);

            int compareFailures = Double.valueOf(thisFailures).compareTo(Double.valueOf(otherFailures));
            if (compareFailures != 0) {
                return -1 * compareFailures;
            }

            //if equal compare without dividing
            int compareDiff = Integer.valueOf(failedTests+diffFailedTests).compareTo(Integer.valueOf(o.failedTests+o.diffFailedTests));
            if (compareDiff != 0) {
                return -1 * compareDiff;
            }

            //never return 0 when using TreeSet
            return -1;
        }

        //
        // reflection methods used by org.antlr.stringtemplate (see daily_report.st files)
        //

        public String getSuiteName() {
            return suite.getName();
        }

        public int getFailedTests() {
            return failedTests;
        }

        public int getDiffFailedTests() {
            return diffFailedTests;
        }

        public boolean isIncreasingDiffFailedTests() {
            return diffFailedTests > 0;
        }

        public boolean isDecreasingDiffFailedTests() {
            return diffFailedTests < 0;
        }

        public int getTotalTests() {
            return totalTests;
        }

        public int getDiffTotalTests() {
            return diffTotalTests;
        }

        public boolean isIncreasingDiffTotalTests() {
            return diffTotalTests > 0;
        }

        public boolean isDecreasingDiffTotalTests() {
            return diffTotalTests < 0;
        }
    }
}
