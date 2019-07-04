package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.AgentDAO;
import com.gigaspaces.newman.spotinst.ElasticGroup;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final MongoClient mongoClient;
    private final AgentDAO agentDAO;
    private final Config config;

    public SpotinstResource(@Context ServletContext servletContext) {
        this.config = Config.fromString(servletContext.getInitParameter("config"));
        spotinstClient = new SpotinstClient();
        mongoClient = new MongoClient(config.getMongo().getHost());
        Morphia morphia = initMorphia();
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
    }


    private Morphia initMorphia() {
        Morphia morphia;
        try {
            morphia = new Morphia().mapPackage("com.gigaspaces.newman.beans.criteria").mapPackage("com.gigaspaces.newman.beans");
        } catch (Exception e) {
            logger.error("failed to init morphia", e);
            throw e;
        }
        return morphia;
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


        return spotinstClient.getAgentsElasticGroups().stream().map(elasticGroup    -> {
            QueryResults<Agent> res = agentDAO.find(agentDAO.createQuery().field("groupName").equal(elasticGroup.getTags().getName()));
            elasticGroup.setConnectedAgents(res.asList().size());
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
