package com.gigaspaces;


import com.gigaspaces.beans.Batch;
import com.gigaspaces.beans.PermResult;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Collection;
import java.util.UUID;


/**
 * Created by Barak Bar Orion
 * <p/>
 * 4/7/15.
 */
public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    public static void main(String[] args) {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder();
            jerseyClientBuilder.register(MultiPartFeature.class);

            Client client = jerseyClientBuilder.build();
            WebTarget target = client.target("http://localhost:8080/api/tests/perm");
            PermResult permResult = new PermResult();
            permResult.setPermutation(UUID.randomUUID().toString());
            PermResult result = target.request().post(Entity.json(permResult), PermResult.class);
            logger.info("post result : {}", result);

            result = target.request().put(Entity.json(permResult), PermResult.class);
            logger.info("put result : {} installed", result);

            Collection<PermResult> results = target.request().get(new GenericType<Collection<PermResult>>() {});
            logger.info("all values are : {}", results);


            target = client.target("http://localhost:8080/api/tests");
            Response response = target.request().get();
            response.bufferEntity();
            Batch batch = response.readEntity(Batch.class);
            logger.info("all ids are : {}", batch);
            logger.info("all ids links : {}", response.getLinks());


//            Client client = jerseyClientBuilder.build();
//            WebTarget target = client.target("http://localhost:8080/api/entry-point/testJSON");
//            Result result = target.request().post(null, Result.class);
//            logger.info("result: {}", result);
//
//            target = client.target("http://localhost:8080/api/entry-point/testJSON1");
//            result = target.request().post(Entity.text("foo"), Result.class);
//            logger.info("result1: {}", result);
//

            final FileDataBodyPart filePart = new FileDataBodyPart("my_pom", new File("pom.xml"));

            final MultiPart multipart = new FormDataMultiPart()
                    .field("foo", "bar")
                    .bodyPart(filePart);
//
            target = client.target("http://localhost:8080/api/tests/post");
            final Response res = target.request()
                    .post(Entity.entity(multipart, multipart.getMediaType()));
            logger.info("response {}", res);


        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}
