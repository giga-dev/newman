package com.gigaspaces.newman.report.daily;

import com.gigaspaces.newman.NewmanClient;
import com.gigaspaces.newman.analytics.Cronable;
import com.gigaspaces.newman.analytics.PropertiesConfigurer;
import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.DashboardData;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.server.NewmanServerConfig;
import com.gigaspaces.newman.smtp.Mailman;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Daily mail report comparing latest build with former build, saving the last compared build-id into a file.
 *
 * Created by moran on 8/13/15.
 */
public class DailyReport implements Cronable {

    private static final Logger logger = LoggerFactory.getLogger(DailyReport.class);

    private static final String DAILY_REPORT_BRANCH = "daily.report.branch";
    private static final String DEFAULT_BRANCH = "master";
    private static final String BID_FILE_SUFFIX = ".bid";

    @Override
    public void run(Properties properties) {

        NewmanServerConfig config = new NewmanServerConfig();
        NewmanClient newmanClient = null;
        try {
            newmanClient = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                    config.getNewmanServerRestUser(), config.getNewmanServerRestPassword());

            StringTemplate htmlTemplate = createHtmlTemplate(properties);

            Build latestBuild = sendEmail(properties, newmanClient, htmlTemplate);

            saveLatestBuildToFIle(properties, latestBuild);


        } catch (Exception e) {
            logger.warn("Failed to execute daily report", e);
        } finally {
            if (newmanClient != null) {
                newmanClient.close();
            }
        }
    }

    private StringTemplate createHtmlTemplate(Properties properties) {
        StringTemplateGroup group = new StringTemplateGroup("group",
                getResourcesPath(properties),
                DefaultTemplateLexer.class);

        return group.getInstanceOf("html-template"); //ref. html-template.st
    }

    private String getResourcesPath(Properties properties) {
        String path = properties.getProperty(PropertiesConfigurer.PROPERTIES_PATH);
        if (path == null) {
            path = Paths.get(".").toAbsolutePath().normalize().toString();
        }
        return path;
    }

    private Build sendEmail(Properties properties, NewmanClient newmanClient, StringTemplate htmlTemplate) throws Exception {

        DashboardData dashboardData = newmanClient.getDashboard().toCompletableFuture().get(1, TimeUnit.MINUTES);

        List<Build> historyBuilds = dashboardData.getHistoryBuilds();
        if (historyBuilds.size() == 0) {
            logger.info("No history builds to generate report");
            return null;
        }

        Build latestBuild = getLatestBuild(properties, historyBuilds);
        Build previousBuild = getPreviousBuildFromFile(properties, latestBuild, newmanClient);

        if (!latestBuild.getBranch().equals(previousBuild.getBranch())) {
            throw new IllegalStateException("Latest build branch doesn't match previous build branch");
        }

        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latestBuild.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previousBuild.getId());
        Set<SuiteDiff> suiteDiffs = compare(latest_mapSuite2Job, previous_mapSuite2Job);
        SuiteDiffSummary summary = getDiffSummary(suiteDiffs);

        final String buildRestUrl = newmanClient.getBaseURI() + "/#!/build/";
        htmlTemplate.setAttribute("summary", summary);
        htmlTemplate.setAttribute("diffs", suiteDiffs);
        htmlTemplate.setAttribute("latestUrl", buildRestUrl + latestBuild.getId());
        htmlTemplate.setAttribute("latestBuildName", latestBuild.getName());
        htmlTemplate.setAttribute("previousUrl", buildRestUrl + previousBuild.getId());
        htmlTemplate.setAttribute("previousBuildName", previousBuild.getName());
        htmlTemplate.setAttribute("latestBuildDate", latestBuild.getBuildTime());
        htmlTemplate.setAttribute("previousBuildDate", previousBuild.getBuildTime());

        String subject = prepareSubject(latestBuild);
        String body = htmlTemplate.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("\nSubject: {}\n\n{}", subject, body);
        }

        Mailman mailman = new Mailman(properties.getProperty(Mailman.MAIL_ACCOUNT_USERNAME),
                properties.getProperty(Mailman.MAIL_ACCOUNT_PASSWORD));

        mailman.compose(properties.getProperty(Mailman.MAIL_MESSAGE_RECIPIENTS), subject, body, Mailman.Format.HTML);

        //save latestBuild for next time we wake up
        return latestBuild;
    }

    private Build getLatestBuild(Properties properties, List<Build> historyBuilds) {
        String branch = properties.getProperty(DAILY_REPORT_BRANCH, DEFAULT_BRANCH);
        for (Build history : historyBuilds) {
            if (history.getBranch().equals(branch)) {
                return history;
            }
        }
        throw new IllegalStateException("No build matching branch: " + branch);
    }

    private Build getPreviousBuildFromFile(Properties properties, Build latestBuild, NewmanClient newmanClient) {
        String path = getResourcesPath(properties);
        String buildIdFile = latestBuild.getBranch() + BID_FILE_SUFFIX; //e.g. master.bid (master previous build id)
        File file = new File(path, buildIdFile);
        try {
            String buildId = org.apache.commons.io.FileUtils.readFileToString(file).trim();
            logger.info("Previous build-id: {}", buildId);
            return newmanClient.getBuild(buildId).toCompletableFuture().get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Failed to get previous build-id from file: {}", file.getAbsolutePath(), e);
        }

        logger.info("Report will not compare latest build with previous build");
        return latestBuild;
    }

    private void saveLatestBuildToFIle(Properties properties, Build latestBuild) {
        String path = getResourcesPath(properties);
        String buildIdFile = latestBuild.getBranch() + BID_FILE_SUFFIX; //e.g. master.bid (master last build id)
        File file = new File(path, buildIdFile);
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file, latestBuild.getId());
        } catch (IOException e) {
            logger.warn("Failed to write latest build-id to file: {}", file.getAbsolutePath(), e);
        }
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
        suiteDiff.totalTests = latestJob.getTotalTests();

        if (previousJob != null) {
            suiteDiff.diffFailedTests = latestJob.getFailedTests() - previousJob.getFailedTests();
            suiteDiff.diffTotalTests = latestJob.getTotalTests() - previousJob.getTotalTests();
        }

        return suiteDiff;
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

}
