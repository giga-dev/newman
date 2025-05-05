package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_setup_logs")
public class JobSetupLog {

    @Id
    private String id;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @Type(type = "com.gigaspaces.newman.types.MapJsonType")
    @Column(name = "agent_logs", columnDefinition = "JSON")
    private Map<String, String> agentLogs = new HashMap<>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getAgentLogs() {
        return agentLogs;
    }

    public void setAgentLogs(Map<String, String> agentLogs) {
        this.agentLogs = agentLogs;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("jobId", job.getId())
                .append("agentLogs", agentLogs)
                .toString();
    }
}
