package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.entities.*;
import com.gigaspaces.newman.utils.EnvUtils;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Boris
 * @since 1.0
 */

public class NewmanSubmitter {

    private static final String NEWMAN_SUITES_FILE_LOCATION = "NEWMAN_SUITES_FILE_LOCATION";
    private static final int MAX_THREADS = 30;
    private static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";
    private static final String NEWMAN_BUILD_TAGS = "NEWMAN_BUILD_TAGS";
    public static final int DEFAULT_TIMEOUT_SECONDS = NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS;
    private static final String RETRY_MINS_INTERVAL_ON_SUSPENDED = "RETRY_MINS_INTERVAL_ON_SUSPENDED";
    private static final int DEFAULT_RETRY_MINS_INTERVAL_ON_SUSPENDED = 1;
    // modes = FORCE, REGULAR
    private static final String NEWMAN_MODE = "NEWMAN_MODE";

    private static final Logger logger = LoggerFactory.getLogger(NewmanSubmitter.class);
    private static Ini properties;

    private NewmanClient newmanClient;
    private String host;
    private String port;
    private String username;
    private String password;
    private ThreadPoolExecutor workers;
    private int intervalOnSuspendMinutes;

    private NewmanSubmitter(String host, String port, String username, String password) {

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.workers = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        intervalOnSuspendMinutes = Integer.valueOf(System.getenv().getOrDefault(RETRY_MINS_INTERVAL_ON_SUSPENDED, String.valueOf(DEFAULT_RETRY_MINS_INTERVAL_ON_SUSPENDED)));

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // connection arguments
        String host = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_HOST, logger);
        String port = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PORT, logger);
        String username = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_USER_NAME, logger);
        String password = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PASSWORD, logger);

        properties = new Ini(new File(EnvUtils.getEnvironment(NEWMAN_SUITES_FILE_LOCATION, logger)));

        NewmanSubmitter newmanSubmitter = new NewmanSubmitter(host, port, username, password);
        String branch = EnvUtils.getEnvironment(NEWMAN_BUILD_BRANCH, false, logger);
        String tags = EnvUtils.getEnvironment(NEWMAN_BUILD_TAGS, false, logger);
        String mode = EnvUtils.getEnvironment(NEWMAN_MODE, false, logger);
        String requiredAgentGroups = properties.get("main").fetch("AGENT_GROUPS_DEFAULT");
        Set<String> agentGroups = parse(requiredAgentGroups);
        String dailyPriorityDefault = properties.get("main").fetch("DAILY_JOB_PRIORITY_DEFAULT");
        String nightlyPriorityDefault = properties.get("main").fetch("NIGHTLY_JOB_PRIORITY_DEFAULT");
        int dailyPriority = Integer.parseInt(dailyPriorityDefault);
        int nightlyPriority = Integer.parseInt(nightlyPriorityDefault);

        if (mode == null || mode.length() == 0) {
            mode = "DAILY";
        }

        int status = newmanSubmitter.start(branch, tags, mode, agentGroups, dailyPriority, nightlyPriority);

        System.exit(status);
    }

    private int start(String branch, String tags, String mode, Set<String> agentGroups, int dailyPriority, int nightlyPriority) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        while (true) {
            ServerStatus serverStatus = newmanClient.getServerStatus().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!serverStatus.getStatus().equals(ServerStatus.Status.RUNNING)) {
                logger.warn("Server is suspended, retrying in " + intervalOnSuspendMinutes + " minutes");
                Thread.sleep(TimeUnit.MINUTES.toMillis(intervalOnSuspendMinutes));
                continue;
            }

            if (mode.equals("NIGHTLY") && isNightlyRequired()) {
                submitJobs(getBuildToRun(branch, tags, mode), getNightlySuitesToSubmit(),getConfigToSubmit(), mode, agentGroups, nightlyPriority);
                properties.get("main").put("LAST_NIGHTLY_RUN", DateTimeFormatter.ofPattern("yyy/MM/dd").format(LocalDate.now()));
                properties.store();
                return 0;
            }

            logger.info("submitting future jobs if there are any");
            boolean hasFutureJobs = submitFutureJobsIfAny();
            logger.info("hasFutureJobs: {}", hasFutureJobs);

            // NOTE exit code = -1 if there are future jobs
            if (hasFutureJobs) {
                tearDown();
                return -1;
            }

            if (mode.equals("DAILY")) {
                Build buildToRun = getBuildToRun(branch, tags, mode);
                if (buildToRun != null) {
                    int numOfRunningJobs = Integer.parseInt(newmanClient.hasRunningJobs().toCompletableFuture().get());
                    if (numOfRunningJobs == 0) {
                        submitJobs(buildToRun, getDailySuiteToSubmit(),getConfigToSubmit(), mode, agentGroups, dailyPriority);
                    }
                }
                return 0;
            }
            return -2;
        }
    }

    private boolean submitFutureJobsIfAny() throws InterruptedException, ExecutionException, TimeoutException {
        // Submit future jobs if exists
        List<Future<String>> jobs = submitFutureJobs();
        if (jobs.isEmpty()) { // if there are no future jobs
            return false;
        } else {
            // wait for every running future job to finish.
            for (Future<String> job : jobs) {
                try {
                    job.get();
                } catch (Exception e) {
                    logger.warn("main thread catch exception of worker: ", e);
                }
            }
            return true;
        }
    }

    private void tearDown() {
        if (newmanClient != null)
            newmanClient.close();
        // shut down job submitters workers
        workers.shutdown();
        try {
            if (!workers.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("workers executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = workers.shutdownNow();
                logger.warn("workers executor was abruptly shut down. {} tasks will not be executed.", droppedTasks.size());
            }
        } catch (InterruptedException e) {
            logger.warn("workers executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = workers.shutdownNow();
            logger.warn("workers executor was abruptly shut down. {} tasks will not be executed.", droppedTasks.size());
        }
    }

    private void submitJobs(Build buildToRun, List<String> suitesId, JobConfig jobConfig, String mode, Set<String> agentGroups, int priority)  {
        List<Future<String>> submitted = new ArrayList<>();
        logger.info("build to run - name:[{}], id:[{}], branch:[{}], tags:[{}], mode:[{}].", buildToRun.getName(), buildToRun.getId(), buildToRun.getBranch(), buildToRun.getTags(), mode);
        // Submit jobs for suites
        try {
            for (String suiteId : filterSuites(suitesId, buildToRun.getId())) {
                submitted.add(submitJobsByThreads(suiteId, buildToRun.getId(), jobConfig.getId(), username, agentGroups, priority));
            }
        } catch (Exception ignored) {
            logger.error("could not submit job. build id- [" + buildToRun + "] on branch :[" + buildToRun.getBranch() + "]", ignored);
        } finally {
            for (Future<String> job : submitted) {
                try {
                    job.get();
                } catch (Exception e) {
                    logger.error("could not submit job", e);
                }
            }
            tearDown();
        }
    }

    private List<String> filterSuites(List<String> suitesId, String buildId) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            List<Suite> staticMiniSuites = newmanClient.getAllSuites().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS).getValues();
            Build build = newmanClient.getBuild(buildId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<String> filteredSuites = new ArrayList<>();
            for (Suite staticSuite : staticMiniSuites) {
                if (suitesId.contains(staticSuite.getId())) {
                    Map<String, String> CustomVariablesMap = Suite.parseCustomVariables(staticSuite.getCustomVariables());
                    String requireBuildStr = CustomVariablesMap.get("REQUIRE_BUILD_TAG");
                    Set<String> requireTags = new HashSet<>();
                    if (requireBuildStr != null) {
                        requireTags = new HashSet<>(Arrays.asList(requireBuildStr.split(",")));
                    }
                    if (build.getTags().containsAll(requireTags)) {
                        filteredSuites.add(staticSuite.getId());
                    }
                }
            }
            return filteredSuites;
        } catch (Exception e) {
            logger.error("can't filter suites. exception: {}", e);
            throw e;
        }
    }

    /**
     * @return List of Future jobs ids.
     */
    private List<Future<String>> submitFutureJobs() throws InterruptedException, ExecutionException, TimeoutException {
        List<Future<String>> futureJobIds = new ArrayList<>();
        FutureJob futureJob = getAndDeleteFutureJob();
        logger.info("submitting future job - " + futureJob);
        while (futureJob != null) {
            Future<String> futureJobWorker = submitJobsByThreads(futureJob.getSuiteID(), futureJob.getBuildID(), futureJob.getConfigID(),futureJob.getAuthor(), futureJob.getAgentGroups(), futureJob.getPriority());
            futureJobIds.add(futureJobWorker);
            futureJob = getAndDeleteFutureJob();
        }
        return futureJobIds;
    }

    private Future<String> submitJobsByThreads(String suiteId, String buildId, String configId, String author, Set<String> agentGroups, int priority) {
        return workers.submit(() -> {
            Suite suite;
            try {
                suite = newmanClient.getSuite(suiteId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (suite == null) {
                    throw new IllegalArgumentException("job suite with id: " + suiteId + " does not exists");
                }
                final NewmanJobSubmitter jobSubmitter = new NewmanJobSubmitter(suiteId, buildId, configId, host, port, username, password, agentGroups, priority);

                String jobId = jobSubmitter.submitJob(author);
                logger.info("submitted job ");
                return jobId;
            } catch (Throwable e) {
                logger.error("submit job faild. SuiteId: " + suiteId+", buildId: "+buildId+", configId: "+configId+", author: "+ author+"agentGroups: "+ agentGroups + "priority: " + priority, e);
                throw e;
            }
        });
    }


    private FutureJob getAndDeleteFutureJob() throws InterruptedException, ExecutionException, TimeoutException {
        FutureJob futureJob;
        try {
            futureJob = newmanClient.getAndDeleteOldestFutureJob().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("failed to submit future job and delete it", e);
            throw e;
        }
        return futureJob;
    }

    private boolean isJobFinished(String jobId) {
        try {
            final Job job = newmanClient.getJob(jobId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (job == null) {
                throw new IllegalArgumentException("No such job with id: " + jobId);
            }
            return job.getState() == State.DONE || job.getState() == State.BROKEN;

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("failed to check if job is finished. exception: {}", e);
            return false;
        }
    }

    private static List<String> getDailySuiteToSubmit() throws IOException {
        Profile.Section main = properties.get("main");
        List<String> suitesList = Arrays.asList((main.fetch("DAILY_SUITES_LIST")).split(","));
        String prevSuiteId = main.fetch("LAST_DAILY_SUITE");
        if (prevSuiteId == null || prevSuiteId.equals("")) {
            prevSuiteId = suitesList.get(suitesList.size() - 1);
            logger.warn("corrupted env - LAST_DAILY_SUITE was empty - running from beginning of DAILY_SUITES_LIST");
        }

        if (suitesList.contains(prevSuiteId)) {
            String nextSuiteId = suitesList.get((suitesList.indexOf(prevSuiteId) + 1) % suitesList.size());
            main.put("LAST_DAILY_SUITE", nextSuiteId);
            properties.store();
            ArrayList<String> result = new ArrayList<>();
            result.add(nextSuiteId);
            return result;
        } else {
            logger.error("corrupted env - LAST_DAILY_SUITE isnt found in DAILY_SUITES_LIST");
            throw new IllegalStateException("corrupted env - LAST_DAILY_SUITE isnt found in DAILY_SUITES_LIST");
        }
    }

    private static List<String> getNightlySuitesToSubmit() {
        Profile.Section main = properties.get("main");
        return Arrays.asList((main.fetch("NIGHTLY_SUITES_LIST")).split(","));
    }

    private static boolean isNightlyRequired() {
        Profile.Section main = properties.get("main");
        String last_nightly_run = main.fetch("LAST_NIGHTLY_RUN");
        if(last_nightly_run == null || last_nightly_run.equals("")){
            return true;
        }
        LocalDate prevRun = LocalDate.parse(last_nightly_run, DateTimeFormatter.ofPattern("yyy/MM/dd"));
        LocalDate now = LocalDate.now();
        return !prevRun.isEqual(now);
    }

    private Build getBuildToRun(String branch, String tags, String mode) {
        try {
            if (mode == null || mode.isEmpty()) {
                mode = "DAILY";
            }
            // tags should be separated by comma (,)
            Build build = newmanClient.getBuildToSubmit(branch, tags, mode).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (build != null) {
                return build;
            }
            logger.warn("failed to find build on branch: {}, tags: {}, mode: {}", branch, tags, mode);
            return null;

        } catch (Exception e) {
            logger.error("failed to get build to run: {}, tags: {}, mode: {}, exception: {}", branch, tags, mode, e);
            return null;
        }
    }

    //"DEFAULT"- defines the default configuration
    private JobConfig getConfigToSubmit() {
        String jobConfigFromFile = properties.get("main").fetch("JOB_CONFIG_DEFAULT");
        JobConfig jobConfig;
        try {
            jobConfig = newmanClient.getConfig(jobConfigFromFile).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if(jobConfig != null) {
                return jobConfig;
            }
            logger.warn("failed to find configuration in DB");
            return null;

        } catch (Exception e) {
            logger.error("failed to find configuration in DB ",e);
            return null;
        }
    }

    private static Set<String> parse(String input){
        Set<String> output =  new TreeSet<>();
        if(input  != null) {
            StringTokenizer st = new StringTokenizer(input, ",");
            while (st.hasMoreTokens())
                output.add(st.nextToken());
        }
        return output;
    }

}
