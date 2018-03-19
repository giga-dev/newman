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

    private static final String CRONS_SUITEDIFF_TAG = "crons.suitediff.tag";
    private static final String CRONS_SUITEDIFF_LATEST_BUILD_ID = "crons.suitediff.latestBuildId";
    private static final String CRONS_SUITEDIFF_PREVIOUS_BUILD_ID = "crons.suitediff.previousBuildId";
    private static final String CRONS_SUITEDIFF_TRACK_LATEST = "crons.suitediff.trackLatest";
    private static final Logger logger = LoggerFactory.getLogger(SuiteDiffCronJob.class);
    private static final String DEFAULT_BRANCH = "master";
    private static final String BID_FILE_SUFFIX = ".bid";
    private static final String AUDIT_FILE_SUFFIX = ".audit";
    private static final String CRONS_SUITEDIFF_BRANCH = "crons.suitediff.branch";

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
        Build previousBuild = getPreviousBuildOrLatest(properties, latestBuild, newmanClient);

        //calculate
        Map<String, Job> latest_mapSuite2Job = getJobsByBuildId(newmanClient, latestBuild.getId());
        Map<String, Job> previous_mapSuite2Job = getJobsByBuildId(newmanClient, previousBuild.getId());
        Set<DiffComparableData> suiteDiffs = compare(latest_mapSuite2Job, previous_mapSuite2Job);
        DiffSummaryData summary = getDiffSummary(suiteDiffs);

        //create subject
        StringTemplate subjectTemplate = createSubjectTemplate(properties);
        subjectTemplate.setAttribute("latestBuildBranch", latestBuild.getBranch());
        subjectTemplate.setAttribute("latestBuildName", latestBuild.getName());
        if (suiteDiffs.size() == 1) { //add single suite name to subject
            DiffComparableData next = suiteDiffs.iterator().next();
            subjectTemplate.setAttribute("suiteName", '\''+next.getSuiteName()+'\'');
        }
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

        htmlTemplate.setAttribute("previousBuildBranch", previousBuild.getBranch());
        htmlTemplate.setAttribute("previousUrl", buildRestUrl + previousBuild.getId());
        htmlTemplate.setAttribute("previousBuildName", previousBuild.getName());
        htmlTemplate.setAttribute("previousBuildDate", simpleDateFormat.format(previousBuild.getBuildTime()));
        htmlTemplate.setAttribute("previousBuildDuration", toHumanReadableDuration(calculateBuildDurationInMillis(previous_mapSuite2Job)));

        final String productName = getProductNameFromTags(latestBuild.getTags());
        if (productName.equals("xap")) {
            htmlTemplate.setAttribute("xapOpenChangeset", getChangeSet("xap-open", previousBuild, latestBuild));
            htmlTemplate.setAttribute("xapChangeset", getChangeSet("xap", previousBuild, latestBuild));
        } else if (productName.equals("insightEdge")) {
            htmlTemplate.setAttribute("insightEdgeChangeset", getChangeSet("InsightEdge", previousBuild, latestBuild));
        }

        List<HistoryTestData> testsThatHaveAHistoryOfFailing = getTestsThatHaveAHistoryOfFailing(latest_mapSuite2Job, newmanClient);
        htmlTemplate.setAttribute("hiss", testsThatHaveAHistoryOfFailing);

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
            saveLatestBuildToFile(properties, latestBuild);
        }

        saveToAuditLogFile(properties, latestBuild);
    }

    private List<HistoryTestData> getTestsThatHaveAHistoryOfFailing(Map<String, Job> latest_mapSuite2Job, NewmanClient newmanClient) throws Exception {
        List<HistoryTestData> failingTests = new ArrayList<>();
        for (Job job : latest_mapSuite2Job.values()) {
            Batch<Test> testBatch = newmanClient.getTests(job.getId(), 0, job.getTotalTests() + job.getNumOfTestRetries()).toCompletableFuture().get();
            for (Test test : testBatch.getValues()) {
                if (test.getStatus().equals(Test.Status.FAIL) && test.getRunNumber() == 3) {
                    String historyStats = test.getHistoryStats();
                    //apply different rules for branch and master
                    if (!job.getBuild().getBranch().equals("master")) {
                        //branch history has delimiter of '_'
                        if (historyStats.startsWith("| _| |") /* match: strike 3! (one on branch and two on master)*/
                                || historyStats.startsWith("| | |") /* match: strike 3! all on branch*/) {
                            addTestToListOfFailingTestsWithHistory(newmanClient, failingTests, job, test);
                        }
                    } else if (historyStats.startsWith("| | |") /* match: strike 3! */) {
                        addTestToListOfFailingTestsWithHistory(newmanClient, failingTests, job, test);
                    }
                }
            }
        }
        return failingTests;
    }

    private void addTestToListOfFailingTestsWithHistory(NewmanClient newmanClient, List<HistoryTestData> failingTests, Job job, Test test) {
        failingTests.add(new HistoryTestData(test, job.getSuite(), newmanClient.getBaseURI() + "/#!/test/" + test.getId()));
    }

    private String getChangeSet(String repository, Build previousBuild, Build latestBuild) {
        String changeSet;
        if (previousBuild.getId().equals(latestBuild.getId())) {
            changeSet = latestBuild.getShas().get(repository); //latest commit
        } else {
            String previousCommit = previousBuild.getShas().get(repository);
            String latestCommit = latestBuild.getShas().get(repository);
            if (previousCommit.equals(latestCommit)) {
                changeSet = latestCommit;
            } else {
                String gitUrl = null;
                if (latestCommit.contains("/commit")) {
                    gitUrl = latestCommit.substring(0, latestCommit.lastIndexOf("/commit"));
                } else if (latestCommit.contains("/tree")) {
                    gitUrl = latestCommit.substring(0, latestCommit.lastIndexOf("/tree"));
                }
                String latestSha = latestCommit.substring(latestCommit.lastIndexOf('/')+1);
                String previousSha = previousCommit.substring(previousCommit.lastIndexOf('/')+1);
                changeSet = gitUrl +"/compare/" + previousSha + "..." + latestSha;
            }
        }
        return changeSet;
    }

    private long calculateBuildDurationInMillis(Map<String, Job> mapSuite2Job) {
        long minStartTime = Long.MAX_VALUE;
        long maxEndTime = 0;
        for (Job job : mapSuite2Job.values()) {
            if( State.DONE.equals(job.getState())) {
                minStartTime = Math.min(minStartTime, job.getStartTime().getTime());
                maxEndTime = Math.max(maxEndTime, job.getEndTime().getTime());
            }
        }
        long totalTime = Math.max(0, maxEndTime - minStartTime);
        return totalTime;
    }

    private String toHumanReadableDuration(long durationInMillis) {

        Map<TimeUnit,Long> result = new LinkedHashMap<>();
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
            //Get full build details since history build object has some columns removed
            history = newmanClient.getBuild(history.getId()).toCompletableFuture().get();
            if (history.getBranch().equals(branch)) {
                if (StringUtils.notEmpty(tag)) {
                    Set<String> tagsToMatch = new HashSet<>(Arrays.asList(tag.split(",")));
                    if (history.getTags().containsAll(tagsToMatch)) {
                        latestMatch = history;
                        break;
                    }
                } else {
                    latestMatch = history;
                    break;
                }
            }
        }
        if (latestMatch != null) {
            logger.info("Latest build-id: {}", latestMatch.getId());
            return latestMatch;
        }
        throw new IllegalStateException("No build matching branch: " + branch + " and tags: "+ tag);
    }

    /**
     * Tries to fetch previous build id from file, if not found will use latest build as previous.
     * If file is found, and id's of previous and latest equals, no report will be generated.
     * Otherwise, returns previous build id
     */
    private Build getPreviousBuildOrLatest(Properties properties, Build latestBuild, NewmanClient newmanClient) throws Exception {
        Build previousBuild;
        try {
            previousBuild = getPreviousBuildFromFile(properties, latestBuild, newmanClient);
        } catch (Exception ignore1) {
            // if latest is a branch build, and it is the first time we run this build, then there is
            // no previous build to compare to; thus compare with master as previous
            if (!latestBuild.getBranch().equals("master")) {
                try {
                    Build masterBranch = new Build();
                    masterBranch.setBranch("master");
                    masterBranch.setTags(latestBuild.getTags());
                    previousBuild = getPreviousBuildFromFile(properties, masterBranch, newmanClient);
                } catch (Exception ignore2) {
                    return latestBuild;
                }
            } else {
                return latestBuild;
            }
        }

        if (latestBuild.getId().equals(previousBuild.getId())) {
            throw new IllegalStateException("Latest and previous build Ids are equal (id=" + latestBuild.getId() + ") - No report will be generated.");
        }

        //read previous from audit log
        String auditBuildId = getPreviousBuildFromAuditFile(properties, previousBuild);
        if (latestBuild.getId().equals(auditBuildId)) {
            throw new IllegalStateException("Latest and previous audit build Ids are equal (id=" + latestBuild.getId() + ") - No report will be generated.");
        }

        return previousBuild;
    }

    private String getTrackedBuildFileName(final Build build, final String suffix) {
        final Set<String> tags = build.getTags();
        String productName = getProductNameFromTags(tags);
        if (productName.length() > 0) {
            return productName + "-" + build.getBranch() + suffix;
        } else {
            return build.getBranch() + suffix;
        }
    }

    private String getProductNameFromTags(Set<String> tags) {
        return tags.contains("XAP") ? "xap" : tags.contains("INSIGHTEDGE") ? "insightEdge" : "";
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
        String buildIdFile = getTrackedBuildFileName(latestBuild, BID_FILE_SUFFIX); //e.g. xap-master.bid (master previous build id)
        File file = new File(path, buildIdFile);
        try {
            String buildId = org.apache.commons.io.FileUtils.readFileToString(file).trim();
            logger.info("Previous build-id: {}", buildId);
            return newmanClient.getBuild(buildId).toCompletableFuture().get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Failed to get previous build-id from file: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }

    private void saveLatestBuildToFile(Properties properties, Build latestBuild) {
        String path = getResourcesPath(properties);
        String buildIdFile = getTrackedBuildFileName(latestBuild, BID_FILE_SUFFIX); //e.g. xap-master.bid (master last build id)
        File file = new File(path, buildIdFile);
        try {
            logger.info("Save latest build-id: {} to file: {}", latestBuild.getId(), file.getAbsolutePath());
            org.apache.commons.io.FileUtils.writeStringToFile(file, latestBuild.getId());
        } catch (IOException e) {
            logger.warn("Failed to write latest build-id to file: {}", file.getAbsolutePath(), e);
        }
    }

    private String getPreviousBuildFromAuditFile(Properties properties, Build latestBuild) throws Exception {
        String path = getResourcesPath(properties);
        String auditFile = getTrackedBuildFileName(latestBuild, AUDIT_FILE_SUFFIX); //e.g. xap-master.audit
        File file = new File(path, auditFile);
        try {
            String buildId = org.apache.commons.io.FileUtils.readFileToString(file).trim();
            logger.info("Previous audit build-id: {}", buildId);
            return buildId;
        } catch (Exception e) {
            logger.warn("Failed to get previous audit build-id from file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private void saveToAuditLogFile(Properties properties, Build latestBuild) {
        String path = getResourcesPath(properties);
        String auditFile = getTrackedBuildFileName(latestBuild, AUDIT_FILE_SUFFIX); //e.g. xap-master.audit
        File file = new File(path, auditFile);
        try {
            logger.info("Save audit build-id: {} to file: {}", latestBuild.getId(), file.getAbsolutePath());
            org.apache.commons.io.FileUtils.writeStringToFile(file, latestBuild.getId());
        } catch (IOException e) {
            logger.warn("Failed to write latest audit build-id to file: {}", file.getAbsolutePath(), e);
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
            if (job.getState().equals(State.DONE)) {
                map.put(job.getSuite().getId(), job);
            }
        }

        return map;
    }

}
