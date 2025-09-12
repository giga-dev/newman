package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.Suite;
import com.gigaspaces.newman.projections.PSuiteThin;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PSuiteDTO implements PSuiteThin {

    private String id;
    private String name;
    private String customVariables;

    public PSuiteDTO(String id, String name, String customVariables) {
        this.id = id;
        this.name = name;
        this.customVariables = customVariables;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCustomVariables() {
        return customVariables;
    }

    public static PSuiteDTO fromEntity(Suite suite) {
        return new PSuiteDTO(suite.getId(), suite.getName(), suite.getCustomVariables());
    }
}
