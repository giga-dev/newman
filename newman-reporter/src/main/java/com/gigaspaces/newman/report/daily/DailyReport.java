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

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

/**
 * Created by moran on 8/13/15.
 */
public class DailyReport implements org.quartz.Job{

    private static final Logger logger = LoggerFactory.getLogger(DailyReport.class);

    private static final String NEWMAN_MAIL_HTML_TEMPLATE_PATH = "newman.mail.html.template.path";

    private static final String NEWMAN_MAIL_PREVIOUS_BUILD_ID = "newman.mail.previous.build.id";


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
        String currentDirPath = Paths.get(".").toAbsolutePath().normalize().toString();
        String templatePath = getNonEmptySystemProperty(NEWMAN_MAIL_HTML_TEMPLATE_PATH, currentDirPath);
        StringTemplateGroup group =  new StringTemplateGroup("group",
                templatePath,
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
        Build previous_build = getPreviousBuild(historyBuilds, buildRef.get(), latest_build, newmanClient);

        if (!latest_build.getBranch().equals(previous_build.getBranch())) {
            logger.info("Latest build branch doesn't match previous build branch");
            return;
        }

        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latest_build.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previous_build.getId());
        Set<SuiteDiff> suiteDiffs = compare(latest_mapSuite2Job, previous_mapSuite2Job);
        SuiteDiffSummary summary = getDiffSummary(suiteDiffs);

        final String buildRestUrl = newmanClient.getBaseURI()+"/#!/build/";
        htmlTemplate.setAttribute("summary", summary);
        htmlTemplate.setAttribute("diffs", suiteDiffs);
        htmlTemplate.setAttribute("latestUrl", buildRestUrl + latest_build.getId());
        htmlTemplate.setAttribute("latestBuildName", latest_build.getName());
        htmlTemplate.setAttribute("previousUrl", buildRestUrl + previous_build.getId());
        htmlTemplate.setAttribute("previousBuildName", previous_build.getName());
        htmlTemplate.setAttribute("latestBuildDate", latest_build.getBuildTime());
        htmlTemplate.setAttribute("previousBuildDate", previous_build.getBuildTime());

        String subject = prepareSubject(latest_build);
        String body = htmlTemplate.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("\nSubject: {}\n\n{}", subject, body);
        }

        Mailman.createHtmlEmail(new MailProperties().setPassword(config.getNewmanMailPassword()),
                config.getNewmanMailUser(), config.getNewmanMailRecipients(), subject, body);

        //save latest_build for next time we wake up
        buildRef.set(latest_build);
    }

    private SuiteDiffSummary getDiffSummary(Set<SuiteDiff> suiteDiffs) {
        SuiteDiffSummary summary = new SuiteDiffSummary();
        for (SuiteDiff diff : suiteDiffs) {
            if (diff.diffFailedTests > 0) {
                summary.totalIncreasingDiff += diff.diffFailedTests;
            } else if (diff.diffFailedTests < 0) {
                summary.totalDecreasingDiff += diff.diffFailedTests;
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

    private Build getPreviousBuild(List<Build> historyBuilds, Build previous_build, Build latest_build, NewmanClient newmanClient) throws ExecutionException, InterruptedException {
        //check sys prop for previous assigned build id
        if (previous_build == null) {
            String previousBuildId = getNonEmptySystemProperty(NEWMAN_MAIL_PREVIOUS_BUILD_ID, null);
            if (previousBuildId != null) {
                previous_build = newmanClient.getBuild(previousBuildId).toCompletableFuture().get();
            }
        }

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
        int totalIncreasingDiff;
        int totalDecreasingDiff;

        //
        // reflection methods used by org.antlr.stringtemplate (see daily_report.st files)
        //

        public int getTotalIncreasingDiff() {
            return totalIncreasingDiff;
        }

        public int getTotalDecreasingDiff() {
            return totalDecreasingDiff;
        }

        public boolean isIncreasingDiff() {
            return totalIncreasingDiff > 0;
        }

        public boolean isDecreasingDiff() {
            return totalDecreasingDiff < 0;
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

            int compareToResult = 0;

            //compare failures as equal disregarding diffs
            compareToResult = Integer.valueOf(failedTests).compareTo(Integer.valueOf(o.failedTests));
            if (compareToResult != 0) {
                return -1 * compareToResult;
            }

            //compare failures as equal disregarding number of tests in suite
            compareToResult = Integer.valueOf(failedTests+diffFailedTests).compareTo(Integer.valueOf(o.failedTests+o.diffFailedTests));
            if (compareToResult != 0) {
                return -1 * compareToResult;
            }

            //compare failed+diff divided by total+diff (ratio of failures)
            double thisFailures = Double.valueOf(failedTests+diffFailedTests).doubleValue() / Double.valueOf(totalTests+diffTotalTests).doubleValue();
            double otherFailures = Double.valueOf(o.failedTests+o.diffFailedTests).doubleValue() / Double.valueOf(o.totalTests+o.diffTotalTests).doubleValue();

            compareToResult = Double.valueOf(thisFailures).compareTo(Double.valueOf(otherFailures));
            if (compareToResult != 0) {
                return -1 * compareToResult;
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
