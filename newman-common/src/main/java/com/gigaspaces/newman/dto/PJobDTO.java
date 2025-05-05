package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.projections.PJob;

import java.net.URI;
import java.util.Date;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PJobDTO implements PJob {
    private String id;
    private PJobDTO.Build build;
    private PJobDTO.Suite suite;
    private PJobDTO.JobConfig jobConfig;
    private Date submitTime;
    private Date startTime;
    private Date endTime;
    private URI testURI;
    private String submittedBy;
    private State state;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int failed3TimesTests;
    private int runningTests;
    private int numOfTestRetries;
    private Set<String> preparingAgents;
    private Set<String> agentGroups;
    private int priority;

    @Override
    public String getId() { return id; }
    public PJobDTO setId(String id) { this.id = id; return this; }

    @Override
    public PJob.Build getBuild() { return build; }
    public PJobDTO setBuild(String id, String name, String branch) {
        this.build = new Build(id, name, branch); return this;
    }

    @Override
    public PJob.Suite getSuite() { return suite; }
    public PJobDTO setSuite(String id, String name) {
        this.suite = new Suite(id,name); return this;
    }

    @Override
    public PJob.JobConfig getJobConfig() { return jobConfig; }
    public PJobDTO setJobConfig(String id, String name) {
        this.jobConfig = new JobConfig(id, name); return this;
    }

    @Override
    public Date getSubmitTime() { return submitTime; }
    public PJobDTO setSubmitTime(Date submitTime) { this.submitTime = submitTime; return this; }

    @Override
    public Date getStartTime() { return startTime; }
    public PJobDTO setStartTime(Date startTime) { this.startTime = startTime; return this; }

    @Override
    public Date getEndTime() { return endTime; }
    public PJobDTO setEndTime(Date endTime) { this.endTime = endTime; return this; }

    @Override
    public URI getTestURI() { return testURI; }
    public PJobDTO setTestURI(URI testURI) { this.testURI = testURI; return this; }

    @Override
    public String getSubmittedBy() { return submittedBy; }
    public PJobDTO setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; return this; }

    @Override
    public State getState() { return state; }
    public PJobDTO setState(State state) { this.state = state; return this; }

    @Override
    public int getTotalTests() { return totalTests; }
    public PJobDTO setTotalTests(int totalTests) { this.totalTests = totalTests; return this; }

    @Override
    public int getPassedTests() { return passedTests; }
    public PJobDTO setPassedTests(int passedTests) { this.passedTests = passedTests; return this; }

    @Override
    public int getFailedTests() { return failedTests; }
    public PJobDTO setFailedTests(int failedTests) { this.failedTests = failedTests; return this; }

    @Override
    public int getFailed3TimesTests() { return failed3TimesTests; }
    public PJobDTO setFailed3TimesTests(int failed3TimesTests) { this.failed3TimesTests = failed3TimesTests; return this; }

    @Override
    public int getRunningTests() { return runningTests; }
    public PJobDTO setRunningTests(int runningTests) { this.runningTests = runningTests; return this; }

    @Override
    public int getNumOfTestRetries() { return numOfTestRetries; }
    public PJobDTO setNumOfTestRetries(int numOfTestRetries) { this.numOfTestRetries = numOfTestRetries; return this; }

    @Override
    public Set<String> getPreparingAgents() { return preparingAgents; }
    public PJobDTO setPreparingAgents(Set<String> preparingAgents) { this.preparingAgents = preparingAgents; return this; }

    @Override
    public Set<String> getAgentGroups() { return agentGroups; }
    public PJobDTO setAgentGroups(Set<String> agentGroups) { this.agentGroups = agentGroups; return this; }

    @Override
    public int getPriority() { return priority; }
    public PJobDTO setPriority(int priority) { this.priority = priority; return this; }

    public static PJobDTO fromEntity(com.gigaspaces.newman.entities.Job job) {
        return new PJobDTO()
                .setId(job.getId())
                .setBuild(job.getBuild().getId(), job.getBuild().getName(), job.getBuild().getBranch())
                .setSuite(job.getSuite().getId(), job.getSuite().getName())
                .setJobConfig(job.getJobConfig().getId(), job.getJobConfig().getName())
                .setSubmitTime(job.getSubmitTime())
                .setStartTime(job.getStartTime())
                .setEndTime(job.getEndTime())
                .setTestURI(job.getTestURI())
                .setSubmittedBy(job.getSubmittedBy())
                .setState(job.getState())
                .setTotalTests(job.getTotalTests())
                .setPassedTests(job.getPassedTests())
                .setFailedTests(job.getFailedTests())
                .setFailed3TimesTests(job.getFailed3TimesTests())
                .setRunningTests(job.getRunningTests())
                .setNumOfTestRetries(job.getNumOfTestRetries())
                .setPreparingAgents(job.getPreparingAgents())
                .setAgentGroups(job.getAgentGroups())
                .setPriority(job.getPriority());
    }

    public static class Build implements PJob.Build{
        private String id;
        private String name;
        private String branch;

        public Build() {}

        public Build(String id, String name, String branch) {
            this.id = id;
            this.name = name;
            this.branch = branch;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
    }

    public static class Suite implements PJob.Suite {
        private String id;
        private String name;

        public Suite() {}

        public Suite(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class JobConfig implements PJob.JobConfig {
        private String id;
        private String name;

        public JobConfig() {}

        public JobConfig(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}

