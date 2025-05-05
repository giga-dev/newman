package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.repository.AgentRepository;
import com.gigaspaces.newman.config.JpaConfig;
import com.gigaspaces.newman.spotinst.ElasticGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Singleton
@Path("/spotinst")
public class SpotinstResource {
    private static final Logger logger = LoggerFactory.getLogger(SpotinstResource.class);
    private final SpotinstClient spotinstClient;
    private final AgentRepository agentRepository;
    private final AnnotationConfigApplicationContext context;

    public SpotinstResource(@Context ServletContext servletContext) {
        this.context = new AnnotationConfigApplicationContext(JpaConfig.class);

        spotinstClient = new SpotinstClient();

        agentRepository = context.getBean(AgentRepository.class);
    }
//
//    @GET
//    @Path("agents/count")
//    @Produces(MediaType.TEXT_PLAIN)
//    public int getAgentsCount() throws IOException {
//        return spotinstClient.getElasticGroupSize();
//    }
//
    @GET
    @Path("elasticgroup")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ElasticGroup> getElasticGroups() throws IOException {


        return spotinstClient.getAgentsElasticGroups().stream().map(elasticGroup -> {
            elasticGroup.setConnectedAgents(agentRepository.countAgentsByGroupName(elasticGroup.getTags().getName()));
            try {
                elasticGroup.setRunningVMs(spotinstClient.getInstancesCountForElasticGroup(elasticGroup.getId()));
            } catch (IOException ignored) {
            }
            return elasticGroup;
        }).collect(Collectors.toList());
    }

    @POST
    @Path("elasticgroup")
    @Produces(MediaType.APPLICATION_JSON)
    public ElasticGroup updateCapacity(@QueryParam("elasticGroupId") String elasticGroupId, @QueryParam("capacity") Integer capacity) throws Exception {
        Optional<String> error = spotinstClient.updateGroupCapacity(elasticGroupId, capacity);
        if (error.isPresent()) {
            throw new Exception(error.get());
        }

        List<ElasticGroup> elasticGroups = getElasticGroups().stream().filter(elasticGroup -> elasticGroup.getId().equals(elasticGroupId)).collect(Collectors.toList());
        if (elasticGroups.size() != 1) {
            throw new Exception("Failed to find elastic group after update capacity (id = " + elasticGroupId);
        }
        return elasticGroups.get(0);
    }
}
