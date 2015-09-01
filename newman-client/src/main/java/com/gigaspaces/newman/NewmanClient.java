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
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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

    public CompletionStage<String> deleteJobUntilDesiredSpace(String requiredFreeDiskSpacePercentage, String numberOfJobs, String diskPartition){
        return restClient.target(uri).path("jobs").path(requiredFreeDiskSpacePercentage).path(numberOfJobs).path(diskPartition).request().rx().delete(String.class);
    }
    public CompletionStage<Batch<Job>> getJobs() {
        return restClient.target(uri).path("job").queryParam("all", "true").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<Batch<Job>> getJobs(String buildId) {
        return restClient.target(uri).path("job").queryParam("buildId", buildId).queryParam("all", "true").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<Job> createJob(JobRequest jobRequest) {
        return restClient.target(uri).path("job").request().rx().put(Entity.json(jobRequest), Job.class);
    }

    public CompletionStage<Test> createTest(Test test) {
        return restClient.target(uri).path("test").request().rx().put(Entity.json(test), Test.class);
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

    public CompletionStage<Test> getTest(String id) {
        return restClient.target(uri).path("test").path(id).request().rx().get(Test.class);
    }


    public CompletionStage<Build> createBuild(Build build) {
        return restClient.target(uri).path("build").request().rx().put(Entity.json(build), Build.class);
    }

    public CompletionStage<Build> getBuild(String id) {
        return restClient.target(uri).path("build").path(id).request().rx().get(Build.class);
    }

    public CompletionStage<Job> subscribe(Agent agent) {
        return restClient.target(uri).path("subscribe").request().rx().post(Entity.json(agent), Job.class);
    }

    public CompletionStage<Batch<Agent>> getSubscriptions(int offset, int limit) {
        return restClient.target(uri).path("subscribe").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Agent>>() {
                });
    }

    public CompletionStage<String> ping(String agentName, String jobId, String testId) {
        return restClient.target(uri).path("ping").path(agentName).path(jobId).path(testId).request().rx().get(String.class);
    }
    public CompletionStage<String> ping(String agentName, String jobId) {
        return restClient.target(uri).path("ping").path(agentName).path(jobId).request().rx().get(String.class);
    }

    public CompletionStage<Agent> getAgent(String agentName) {
        return restClient.target(uri).path("agent").path(agentName).request().rx().get(Agent.class);
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
        return restClient.target(uri).path("suite").request().rx().post(Entity.json(suite), Suite.class);
    }

    public CompletionStage<Suite> getSuite(String id) {
        return restClient.target(uri).path("suite").path(id).request().rx().get(Suite.class);
    }

    public CompletionStage<Batch<Suite>> getAllSuites() {
        return restClient.target(uri).path("suite").request().rx().get(new GenericType<Batch<Suite>>() {
        });
    }

    public CompletionStage<Test> getTestToRun(Agent agent) {
        return restClient.target(uri).path("agent").request().rx().post(Entity.json(agent), Test.class);
    }

    public CompletionStage<DashboardData> getDashboard() {
        return restClient.target(uri).path("dashboard").request().rx().get(DashboardData.class);
    }

    public void close() {
        restClient.close();
    }


    public CompletionStage<Test> uploadLog( String jobId, String testId, File log) {
        final FileDataBodyPart filePart = new FileDataBodyPart("file", log);

        final MultiPart multipart = new FormDataMultiPart()
//                .field("foo", "bar")
                .bodyPart(filePart);

//        @Path("test/{id}/log")
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

}
