package com.gigaspaces.newman.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 5/11/15.
 */
public class BuildsComparisonDTO {
    private String[] buildLeftDetails;
    private String[] buildRightDetails;
    private Map<String, ResultsLeftRightDTO> buildResults;

    public BuildsComparisonDTO() {
        buildResults = new HashMap<>();
    }

    public BuildsComparisonDTO(String[] buildLeftDetails, String[] buildRightDetails) {
        this();
        this.buildLeftDetails = buildLeftDetails;
        this.buildRightDetails = buildRightDetails;
    }

    public String[] getBuildLeftDetails() {
        return buildLeftDetails;
    }

    public void setBuildLeftDetails(String[] buildLeftDetails) {
        this.buildLeftDetails = buildLeftDetails;
    }

    public String[] getBuildRightDetails() {
        return buildRightDetails;
    }

    public void setBuildRightDetails(String[] buildRightDetails) {
        this.buildRightDetails = buildRightDetails;
    }

    public Map<String, ResultsLeftRightDTO> getBuildResults() {
        return buildResults;
    }

    public void setBuildResults(Map<String, ResultsLeftRightDTO> buildResults) {
        this.buildResults = buildResults;
    }

    public void addSuiteLeft(String suiteName, String jobId, int total, int passed, int failed, int failed3) {
        getBuildResults().compute(suiteName, (key, value) -> {
            JobStatsDTO jobStats = new JobStatsDTO(jobId, total, passed, failed, failed3);
            if (value == null) {
                return new ResultsLeftRightDTO().setLeft(jobStats);
            } else {
                return value.setLeft(jobStats);
            }
        });
    }

    public void addSuiteRight(String suiteName, String jobId, int total, int passed, int failed, int failed3) {
        getBuildResults().compute(suiteName, (key, value) -> {
            JobStatsDTO jobStats = new JobStatsDTO(jobId, total, passed, failed, failed3);
            if (value == null) {
                return new ResultsLeftRightDTO().setRight(jobStats);
            } else {
                return value.setRight(jobStats);
            }
        });
    }

    private static class JobStatsDTO {

        private String jobId;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int failed3TimesTests;

        public JobStatsDTO() {
        }

        public JobStatsDTO(String jobId, int totalTests, int passedTests, int failedTests, int failed3TimesTests) {
            this.jobId = jobId;
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.failed3TimesTests = failed3TimesTests;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public int getTotalTests() {
            return totalTests;
        }

        public void setTotalTests(int totalTests) {
            this.totalTests = totalTests;
        }

        public int getPassedTests() {
            return passedTests;
        }

        public void setPassedTests(int passedTests) {
            this.passedTests = passedTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public void setFailedTests(int failedTests) {
            this.failedTests = failedTests;
        }

        public int getFailed3TimesTests() {
            return failed3TimesTests;
        }

        public void setFailed3TimesTests(int failed3TimesTests) {
            this.failed3TimesTests = failed3TimesTests;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("jobId", jobId)
                    .append("totalTests", totalTests)
                    .append("passedTests", passedTests)
                    .append("failedTests", failedTests)
                    .append("failed3TimesTests", failed3TimesTests)
                    .toString();
        }
    }

    private static class ResultsLeftRightDTO {
        JobStatsDTO left;
        JobStatsDTO right;

        public ResultsLeftRightDTO() {
        }

        public JobStatsDTO getLeft() {
            return left;
        }

        public ResultsLeftRightDTO setLeft(JobStatsDTO left) {
            this.left = left;
            return this;
        }

        public JobStatsDTO getRight() {
            return right;
        }

        public ResultsLeftRightDTO setRight(JobStatsDTO right) {
            this.right = right;
            return this;
        }
    }
}
