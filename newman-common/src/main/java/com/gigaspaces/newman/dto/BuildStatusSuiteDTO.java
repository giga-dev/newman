package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.BuildStatusSuite;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildStatusSuiteDTO {

    private String id;
    private String suiteId;
    private String suiteName;

    // Constructor
    public BuildStatusSuiteDTO(String id, String suiteId, String suiteName) {
        this.id = id;
        this.suiteId = suiteId;
        this.suiteName = suiteName;
    }

    // Conversion method to convert BuildStatusSuite entity to BuildStatusSuiteDTO
    public static BuildStatusSuiteDTO fromEntity(BuildStatusSuite buildStatusSuite) {
        return new BuildStatusSuiteDTO(
                buildStatusSuite.getId(),
                buildStatusSuite.getSuiteId(),
                buildStatusSuite.getSuiteName()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }
}

