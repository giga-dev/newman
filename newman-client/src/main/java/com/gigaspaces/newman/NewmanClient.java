package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.RxWebTarget;
import org.glassfish.jersey.client.rx.java8.RxCompletionStage;
import org.glassfish.jersey.client.rx.java8.RxCompletionStageInvoker;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Created by Barak Bar Orion
 * 4/19/15.
 */
public class NewmanClient {

    private final RxClient<RxCompletionStageInvoker> restClient;
    private final String uri;

    public NewmanClient(RxClient<RxCompletionStageInvoker> restClient, String uri) {
        this.restClient = restClient;
        this.uri = uri;
    }

    /**
     * @return e.g. https://xap-newman:8443
     */
    public String getBaseURI() {
        return uri.substring(0, uri.indexOf("/api/newman"));
    }

    public CompletionStage<Batch<Job>> getJobs(int offset, int limit) {
        return restClient.target(uri).path("job").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Batch<Job>> getJobs(int limit) {
        return restClient.target(uri).path("job").queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Job> getJob(String jobId) {
        return restClient.target(uri).path("job").path(jobId).request().rx().get(Job.class);
    }

    public CompletionStage<String> deleteJobUntilDesiredSpace(String nubmerOfDaysToNotDelete) {
        return restClient.target(uri).path("jobs").path(nubmerOfDaysToNotDelete).request().rx().delete(String.class);
    }

    public CompletionStage<Batch<Job>> getJobs() {
        return restClient.target(uri).path("job").queryParam("all", "true").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<Batch<Job>> getJobs(String buildId) {
        return restClient.target(uri).path("job").queryParam("buildId", buildId).queryParam("all", "true").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<String> hasRunningJobs() {
        return restClient.target(uri).path("job").path("running").request().rx().get(String.class);
    }

    public CompletionStage<Job> createJob(JobRequest jobRequest) {
        return restClient.target(uri).path("job").request().rx().put(Entity.json(jobRequest), Job.class);
    }

    public CompletionStage<List<FutureJob>> createFutureJob(FutureJobsRequest futureJobRequest) {
       return restClient.target(uri).path("futureJob").request().rx().post(Entity.json(futureJobRequest),new GenericType<List<FutureJob>>(){});
    }

    public CompletionStage<Boolean> hasHigherPriorityJob(String agentId, String jobId) {
        return restClient.target(uri).path("prioritizedJob").path(agentId).path(jobId).request().rx().get(Boolean.class);
    }

    public CompletionStage<Job> changeJob(String jobId, int newPriority, Set<String> agentGroups) {
        EditJobRequest editJobRequest = new EditJobRequest();
        editJobRequest.setAgentGroups(agentGroups);
        editJobRequest.setPriority(newPriority);
        return restClient.target(uri).path("job").path(jobId).path("edit").request().rx().post(Entity.json(editJobRequest), Job.class);
    }

    public CompletionStage<Test> createTest(Test test) {
        return restClient.target(uri).path("test").request().rx().put(Entity.json(test), Test.class);
    }

    public CompletionStage<Response> createTests(List<Test> tests, String queryParam) {
        return restClient.target(uri).path("tests").queryParam("toCount", queryParam).request().rx().put(Entity.json(new Batch<>(tests, 0, tests.size(), false, null, null)));
    }

    public CompletionStage<ServerStatus> getServerStatus() {
        return restClient.target(uri).path("status").request().rx().get(ServerStatus.class);
    }

    public CompletionStage<Build> getBuildToSubmit(String branch, String tags, String mode) {
        return restClient.target(uri).path("build-to-submit").
                queryParam("branch", branch).
                queryParam("tags", tags).
                queryParam("mode", mode).
                request().rx().get(Build.class);
    }

    public CompletionStage<Build> cacheBuildInServer(String buildIdToRun){
        return restClient.target(uri).
                path("cacheBuild").
                queryParam("buildIdToCache", buildIdToRun).
                request().rx().get(Build.class);
    }

    public CompletionStage<Test> finishTest(Test test) {
        return restClient.target(uri).path("test").request().rx().post(Entity.json(test), Test.class);
    }

    public CompletionStage<Batch<Test>> getTests(String jobId, int offset, int limit) {
        return restClient.target(uri).path("test").queryParam("offset", offset).queryParam("limit", limit)
                .queryParam("jobId", jobId).request()
                .rx().get(new GenericType<Batch<Test>>() {
                });
    }

    public CompletionStage<Batch<TestHistoryItem>> getTestHistory(String testId) {
        return restClient.target(uri).path("test-history").queryParam("id", testId).request()
                .rx().get(new GenericType<Batch<TestHistoryItem>>() {
                });
    }

    public CompletionStage<Build> createBuild(Build build) {
        return restClient.target(uri).path("build").request().rx().put(Entity.json(build), Build.class);
    }

    public CompletionStage<Build> getBuild(String id) {
        return restClient.target(uri).path("build").path(id).request().rx().get(Build.class);
    }

    public CompletionStage<FutureJob> getAndDeleteOldestFutureJob() {
        return restClient.target(uri).path("futureJob").request().rx().get(FutureJob.class);
    }

    public CompletionStage<Build> getLatestBuild(String tags) {
        if (tags == null){
            tags = "";
        }
        return restClient.target(uri).path("build").path("latest").path(tags).request().rx().get(Build.class);
    }

    public CompletionStage<Batch<Build>> getLatestBuilds( String branch, String tags, int limit, boolean withAllJobsCompleted ) {
        return restClient.target(uri).path("latest-builds")
            .queryParam("branch", branch)
            .queryParam("tags", tags)
            .queryParam("limit", limit)
            .queryParam("with-all-jobs-completed", withAllJobsCompleted)
            .request().rx().get(new GenericType<Batch<Build>>() {
        });
    }

    public CompletionStage<Job> subscribe(Agent agent) {
        return restClient.target(uri).path("subscribe").request().rx().post(Entity.json(agent), Job.class);
    }

    public CompletionStage<Job> agentFinishJob(String jobId) {
        return restClient.target(uri).path("job").path(jobId).request().rx().post(Entity.text(""), Job.class);
    }

    public CompletionStage<Batch<Agent>> getSubscriptions(int offset, int limit) {
        return restClient.target(uri).path("subscribe").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Agent>>() {
                });
    }

    public CompletionStage<String> ping(String agentName) {
        return restClient.target(uri).path("ping").path(agentName).path("-").request().rx().get(String.class);
    }

    public CompletionStage<String> ping(String agentName, String jobId) {
        return restClient.target(uri).path("ping").path(agentName).path(jobId).request().rx().get(String.class);
    }

    public CompletionStage<Agent> getAgent(String agentName) {
        return restClient.target(uri).path("agent").path(agentName).request().rx().get(Agent.class);
    }

    public CompletionStage<Agent> setSetupRetries(Agent agent, int setupRetries) {
        return restClient.target(uri).path("agent").path(Integer.toString(setupRetries)).request().rx().post(Entity.json(agent), Agent.class);
    }

    public CompletionStage<Test> getReadyTest(String agentName, String jobId) {
        return restClient.target(uri).path("agent").path(agentName).path(jobId).request().rx().post(Entity.text(""), Test.class);
    }

    public CompletionStage<Job> unsubscribe(Agent agent) {
        return restClient.target(uri).path("unsubscribe").request().rx().post(Entity.json(agent), Job.class);
    }

    public CompletionStage<Agent> freeAgent(String agentName) {
        return restClient.target(uri).path("freeAgent").path(agentName).request().rx().post(Entity.text(""), Agent.class);
    }

    public CompletionStage<Suite> addSuite(Suite suite) {
        return restClient.target(uri).path("suite").request().rx().post(Entity.json(suite), Suite.class); //Todo - not consists with newman resource
    }

    public CompletionStage<Suite> getSuite(String id) {
        return restClient.target(uri).path("suite").path(id).request().rx().get(Suite.class);
    }

    public CompletionStage<Batch<Suite>> getAllSuites() {
        return restClient.target(uri).path("suite").request().rx().get(new GenericType<Batch<Suite>>() {
        });
    }

    public CompletionStage<Suite> deleteSuite(String suiteId) {
        return restClient.target(uri).path("suite").path(suiteId).request().rx().delete(Suite.class);
    }

    public CompletionStage<Suite> updateSuite(Suite suite) {
        return restClient.target(uri).path("update-suite").request().rx().post(Entity.json(suite), Suite.class);
    }

    public CompletionStage<Test> getTestToRun(Agent agent) {
        return restClient.target(uri).path("agent").request().rx().post(Entity.json(agent), Test.class);
    }

    public CompletionStage<DashboardData> getDashboard() {
        return restClient.target(uri).path("dashboard").request().rx().get(DashboardData.class);
    }

    public CompletionStage<String> updateSha() {
        return restClient.target(uri).path("update-sha").request().rx().get(String.class);
    }

    public CompletionStage<JobConfig> addConfig(JobConfig config) {
        return restClient.target(uri).path("job-config").request().rx().post(Entity.json(config), JobConfig.class);
    }

    public CompletionStage<JobConfig> getConfig(String name) {
        return restClient.target(uri).path("job-config").path(name).request().rx().get(JobConfig.class);
    }

    public CompletionStage<JobConfig> getConfigById(String id) {
        return restClient.target(uri).path("job-config-by-id").path(id).request().rx().get(JobConfig.class);
    }

    public CompletionStage<Set<String>> getAvailableAgentGroups() {
        return restClient.target(uri).path("availableAgentGroups").request().rx().get(new GenericType<Set<String>>(){});
    }

    public CompletionStage<List<JobConfig>> getAllConfigs() {
        return restClient.target(uri).path("job-config").request().rx().get(new GenericType<List<JobConfig>>(){});
    }

    public void close() {
        restClient.close();
    }


    public CompletionStage<Job> uploadJobSetupLog(String jobId, String agentName, File log) {
        final FileDataBodyPart filePart = new FileDataBodyPart("file", log);

        final MultiPart multipart = new FormDataMultiPart()
                .bodyPart(filePart);

        RxWebTarget<RxCompletionStageInvoker> target = restClient.target(uri).path("job").path(jobId).path(agentName).path("log");
        return target.request().rx()
                .post(Entity.entity(multipart, multipart.getMediaType()), Job.class);

    }

    public CompletionStage<Test> uploadTestLog(String jobId, String testId, File log) {
        final FileDataBodyPart filePart = new FileDataBodyPart("file", log);

        final MultiPart multipart = new FormDataMultiPart()
                .bodyPart(filePart);

        RxWebTarget<RxCompletionStageInvoker> target = restClient.target(uri).path("test").path(jobId).path(testId).path("log");
        return target.request().rx()
                .post(Entity.entity(multipart, multipart.getMediaType()), Test.class);

    }

    public EventInput getEventInput() {
        return restClient.target(uri).path("event").request().get(EventInput.class);
    }

    public static NewmanClient create(String host, String port, String user, String pw) throws KeyManagementException, NoSuchAlgorithmException {
        final String URI = "https://" + host + ":" + port + "/api/newman";
        JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder()
                .sslContext(SSLContextFactory.acceptAll())
                .hostnameVerifier((s, sslSession) -> true)
                .register(MultiPartFeature.class).register(SseFeature.class)
                .register(HttpAuthenticationFeature.basic(user, pw));

        RxClient<RxCompletionStageInvoker> restClient = RxCompletionStage.from(jerseyClientBuilder.build());
        return new NewmanClient(restClient, URI);
    }

    public CompletionStage<Build> appendToBuild(Build build) {
        return restClient.target(uri).path("build").request().rx().post(Entity.json(build), Build.class);
    }

    //calls the creates suite from failing test option in UI
    public CompletionStage<Suite> createSuiteFromFailingTests(String jobId, String suiteName) {
        return restClient.target(uri).path("suite").path("failedTests").queryParam("jobId",jobId)
                .queryParam("suiteName",suiteName).request().rx().post(Entity.text(""), Suite.class);
    }
}
