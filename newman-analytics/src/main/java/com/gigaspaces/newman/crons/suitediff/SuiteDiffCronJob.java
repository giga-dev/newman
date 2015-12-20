package com.gigaspaces.newman.crons.suitediff;

import com.gigaspaces.newman.NewmanClient;
import com.gigaspaces.newman.analytics.CronJob;
import com.gigaspaces.newman.analytics.PropertiesConfigurer;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.server.NewmanServerConfig;
import com.gigaspaces.newman.smtp.Mailman;
import com.gigaspaces.newman.utils.StringUtils;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * sends an email report comparing latest build with former build, saving the last compared build-id into a file.
 *
 * Created by moran on 8/13/15.
 */
public class SuiteDiffCronJob implements CronJob {

    private static final Logger logger = LoggerFactory.getLogger(SuiteDiffCronJob.class);

    private static final String DEFAULT_BRANCH = "master";
    private static final String BID_FILE_SUFFIX = ".bid";
    private static final String CRONS_SUITEDIFF_BRANCH = "crons.suitediff.branch";
    public static final String CRONS_SUITEDIFF_TAG = "crons.suitediff.tag";
    public static final String CRONS_SUITEDIFF_LATEST_BUILD_ID = "crons.suitediff.latestBuildId";
    public static final String CRONS_SUITEDIFF_PREVIOUS_BUILD_ID = "crons.suitediff.previousBuildId";
    public static final String CRONS_SUITEDIFF_TRACK_LATEST = "crons.suitediff.trackLatest";

    @Override
    public void run(Properties properties) {

        NewmanServerConfig config = new NewmanServerConfig();
        NewmanClient newmanClient = null;
        try {
            newmanClient = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                    config.getNewmanServerRestUser(), config.getNewmanServerRestPassword());

            sendEmail(properties, newmanClient);

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

        return group.getInstanceOf("body-template"); //ref. body-template.st
    }

    private StringTemplate createSubjectTemplate(Properties properties) {
        StringTemplateGroup group = new StringTemplateGroup("group",
                getResourcesPath(properties),
                DefaultTemplateLexer.class);

        return group.getInstanceOf("subject-template"); //ref. subject-template.st
    }

    private String getResourcesPath(Properties properties) {
        String path = properties.getProperty(PropertiesConfigurer.PROPERTIES_PATH);
        if (path == null) {
            path = Paths.get(".").toAbsolutePath().normalize().toString();
        }
        return path;
    }

    private void sendEmail(Properties properties, NewmanClient newmanClient) throws Exception {

        DashboardData dashboardData = newmanClient.getDashboard().toCompletableFuture().get(1, TimeUnit.MINUTES);

        List<Build> historyBuilds = dashboardData.getHistoryBuilds();
        if (historyBuilds.size() == 0) {
            logger.info("No history builds to generate report");
            return;
        }

        Build latestBuild = getLatestBuild(properties, historyBuilds, newmanClient);
        Build previousBuild = getPreviousBuildFromFile(properties, latestBuild, newmanClient);

        //calculate
        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latestBuild.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previousBuild.getId());
        Set<DiffComparableData> suiteDiffs = compare(latest_mapSuite2Job, previous_mapSuite2Job);
        DiffSummaryData summary = getDiffSummary(suiteDiffs);

        //create subject
        StringTemplate subjectTemplate = createSubjectTemplate(properties);
        subjectTemplate.setAttribute("latestBuildBranch", latestBuild.getBranch());
        subjectTemplate.setAttribute("latestBuildName", latestBuild.getName());
        subjectTemplate.setAttribute("latestBuildFailedTests", latestBuild.getBuildStatus().getFailedTests());

        //create body
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE d MMM, HH:mm");
        StringTemplate htmlTemplate = createHtmlTemplate(properties);
        String buildRestUrl = newmanClient.getBaseURI() + "/#!/build/";
        htmlTemplate.setAttribute("summary", summary);
        htmlTemplate.setAttribute("diffs", suiteDiffs);

        htmlTemplate.setAttribute("latestBuildBranch", latestBuild.getBranch());
        htmlTemplate.setAttribute("latestUrl", buildRestUrl + latestBuild.getId());
        htmlTemplate.setAttribute("latestBuildName", latestBuild.getName());
        htmlTemplate.setAttribute("latestBuildDate", simpleDateFormat.format(latestBuild.getBuildTime()));
        htmlTemplate.setAttribute("latestBuildDuration", toHumanReadableDuration(calculateBuildDurationInMillis(latest_mapSuite2Job)));
        calculateBuildDurationInMillis(latest_mapSuite2Job);

        htmlTemplate.setAttribute("previousBuildBranch", previousBuild.getBranch());
        htmlTemplate.setAttribute("previousUrl", buildRestUrl + previousBuild.getId());
        htmlTemplate.setAttribute("previousBuildName", previousBuild.getName());
        htmlTemplate.setAttribute("previousBuildDate", simpleDateFormat.format(previousBuild.getBuildTime()));
        htmlTemplate.setAttribute("previousBuildDuration", toHumanReadableDuration(calculateBuildDurationInMillis(previous_mapSuite2Job)));

        //send mail
        String subject = subjectTemplate.toString();
        String body = htmlTemplate.toString();

        if (logger.isDebugEnabled()) {
            logger.debug("\nSubject: {}\n\n{}", subject, body);
        }

        Mailman mailman = new Mailman(properties.getProperty(Mailman.MAIL_ACCOUNT_USERNAME),
                properties.getProperty(Mailman.MAIL_ACCOUNT_PASSWORD));

        mailman.compose(properties.getProperty(Mailman.MAIL_MESSAGE_RECIPIENTS), subject, body, Mailman.Format.HTML);

        //save latestBuild for next time we wake up
        if (Boolean.parseBoolean(properties.getProperty(CRONS_SUITEDIFF_TRACK_LATEST, "true"))) {
            // save only if branches match otherwise keep comparison fixed to whatever was placed in file
            if (latestBuild.getBranch().equals(previousBuild.getBranch())) {
                logger.info("Saving latest buildId to file");
                saveLatestBuildToFile(properties, latestBuild);
            }
        }
    }

    private long calculateBuildDurationInMillis(Map<String, Job> mapSuite2Job) {
        long totalTime = 0;
        for (Job job : mapSuite2Job.values()) {
            totalTime += (job.getEndTime().getTime() - job.getStartTime().getTime());
        }
        return totalTime;
    }

    private String toHumanReadableDuration(long durationInMillis) {

        Map<TimeUnit,Long> result = new LinkedHashMap<TimeUnit,Long>();
        TimeUnit[] units = new TimeUnit[]{TimeUnit.HOURS, TimeUnit.MINUTES};
        long restOfDurationInMillis = durationInMillis;
        for ( TimeUnit unit : units ) {
            long diff = unit.convert(restOfDurationInMillis,TimeUnit.MILLISECONDS);
            long diffInMillisForUnit = unit.toMillis(diff);
            restOfDurationInMillis = restOfDurationInMillis - diffInMillisForUnit;
            result.put(unit,diff);
        }

        StringBuilder output = new StringBuilder();
        for (Map.Entry<TimeUnit, Long> timeUnitLongEntry : result.entrySet()) {
            output.append(timeUnitLongEntry.getValue()).append(timeUnitLongEntry.getKey().toString().toLowerCase().charAt(0)).append(" ");
        }
        return output.toString();
    }

    private Build getLatestBuild(Properties properties, List<Build> historyBuilds, NewmanClient newmanClient) throws Exception {
        String latestBuildIdOverride = properties.getProperty(CRONS_SUITEDIFF_LATEST_BUILD_ID);
        if (latestBuildIdOverride != null) {
            logger.info("Fetching latest build-id: {}", latestBuildIdOverride);
            Build build = newmanClient.getBuild(latestBuildIdOverride).toCompletableFuture().get();
            if (build == null) {
                throw new IllegalStateException("latest build (id="+latestBuildIdOverride+") was not found");
            }
            return build;
        }
        String branch = properties.getProperty(CRONS_SUITEDIFF_BRANCH, DEFAULT_BRANCH);
        String tag = properties.getProperty(CRONS_SUITEDIFF_TAG);
        Build latestMatch = null;
        for (Build history : historyBuilds) {
            if (history.getBranch().equals(branch)) {
                if (latestMatch == null) {
                    latestMatch = history;
                }
                if (StringUtils.notEmpty(tag) && history.getTags().contains(tag)) {
                    //temporary workaround - to identify nightly suite
                    if (tag.equals("DOTNET")) {
                        if (countSuites(newmanClient, history) > 5) {
                            return history;
                        }
                    } else {
                        return history;
                    }
                }
            }
        }
        if (latestMatch != null) {
            logger.info("Latest build-id: {}", latestMatch.getId());
            return latestMatch;
        }
        throw new IllegalStateException("No build matching branch: " + branch);
    }

    /**
     * Temporary workaround to count number of suites of running builds
     * @return the number of suites run on this build
     */
    private int countSuites(NewmanClient newmanClient, Build build) {
        int suiteCount = 0;
        try {
            Batch<Job> jobBatch = newmanClient.getJobs(build.getId()).toCompletableFuture().get();
            suiteCount = jobBatch.getValues().size();
        } catch (Exception e) {
            logger.warn("Failed to get jobs for buildId: " + build.getId());
        }
        return suiteCount;
    }

    private Build getPreviousBuildFromFile(Properties properties, Build latestBuild, NewmanClient newmanClient) throws Exception {
        String previousBuildIdOverride = properties.getProperty(CRONS_SUITEDIFF_PREVIOUS_BUILD_ID);
        if (previousBuildIdOverride != null) {
            logger.info("Fetching previous build-id: {}", previousBuildIdOverride);
            Build build = newmanClient.getBuild(previousBuildIdOverride).toCompletableFuture().get();
            if (build == null) {
                throw new IllegalStateException("previous build (id="+previousBuildIdOverride+") was not found");
            }
            return build;
        }

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

    private void saveLatestBuildToFile(Properties properties, Build latestBuild) {
        String path = getResourcesPath(properties);
        String buildIdFile = latestBuild.getBranch() + BID_FILE_SUFFIX; //e.g. master.bid (master last build id)
        File file = new File(path, buildIdFile);
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file, latestBuild.getId());
        } catch (IOException e) {
            logger.warn("Failed to write latest build-id to file: {}", file.getAbsolutePath(), e);
        }
    }

    private DiffSummaryData getDiffSummary(Set<DiffComparableData> suiteDiffs) {
        DiffSummaryData summary = new DiffSummaryData();
        for (DiffComparableData diff : suiteDiffs) {
            if (diff.diffFailedTests > 0) {
                summary.totalIncreasingDiff += diff.diffFailedTests;
            } else if (diff.diffFailedTests < 0) {
                summary.totalDecreasingDiff += diff.diffFailedTests;
            }
        }
        return summary;
    }

    private Set<DiffComparableData> compare(Map<String, Job> latest_mapSuite2Job, Map<String, Job> previous_mapSuite2Job) {
        Set<DiffComparableData> set = new TreeSet<>();
        for (Map.Entry<String, Job> entry : latest_mapSuite2Job.entrySet()) {
            String suiteId = entry.getKey();
            Job latestJob = entry.getValue();
            Job previousJob = previous_mapSuite2Job.get(suiteId);
            set.add(createSuiteDiff(latestJob, previousJob));
        }

        return set;
    }

    private DiffComparableData createSuiteDiff(Job latestJob, Job previousJob) {
        DiffComparableData suiteDiff = new DiffComparableData();
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
        Map<String, Job> map = new HashMap<>();
        Batch<Job> jobBatch = newmanClient.getJobs(buildId).toCompletableFuture().get();
        for (Job job : jobBatch.getValues()) {
            map.put(job.getSuite().getId(), job);
        }

        return map;
    }

}
