package com.gigaspaces.newman;


import com.gigaspaces.newman.spotinst.*;
import com.google.gson.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.gigaspaces.newman.utils.StringUtils.getRequiredSystemProperty;

public class SpotinstClient {
    private static final String NEWMAN_SERVER_SPOTINST_TOKEN = "newman.server.spotinst.token";
    private static final String NEWMAN_SERVER_SPOTINST_ACCOUNT_ID = "newman.server.spotinst.accountId";

    private final CloseableHttpClient client;
    private final String ACCOUNT_ID;
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(ElasticGroupTags.class, new ElasticGroupTagsDeserializer())
            .create();
    private static JsonParser parser = new JsonParser();


    public SpotinstClient() {
        ACCOUNT_ID = getRequiredSystemProperty(NEWMAN_SERVER_SPOTINST_ACCOUNT_ID);
        String TOKEN = getRequiredSystemProperty(NEWMAN_SERVER_SPOTINST_TOKEN);

        List<Header> defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader("Content-Type", "application/json"));
        defaultHeaders.add(new BasicHeader("Authorization", "Bearer " + TOKEN));
        client = HttpClientBuilder.create().setDefaultHeaders(defaultHeaders).build();

    }

    public void test() throws IOException {
        getAgentsElasticGroups().stream().forEach(group -> {
            System.out.println(group);
        });
    }


    public static void main(String[] args) throws IOException {
        new SpotinstClient().test();
    }

    public List<ElasticGroup> getAgentsElasticGroups() throws IOException {
        String url = String.format("https://api.spotinst.io/aws/ec2/group?accountId=%s", ACCOUNT_ID);
        HttpResponse response = executeGet(url);
        JsonObject data = parseResponse(response);

        JsonArray items = data.get("response").getAsJsonObject().get("items").getAsJsonArray();
        return StreamSupport.stream(items.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(group -> group.get("name").getAsString().startsWith("Automation Newman Agent"))
                .map(group -> gson.fromJson(group, ElasticGroup.class))
                .collect(Collectors.toList());
    }

    public Optional<String> updateGroupCapacity(String elasticGroupId, int target) throws IOException {
        String url = String.format("https://api.spotinst.io/aws/ec2/group/%s/capacity?accountId=%s", elasticGroupId, ACCOUNT_ID);
        UpdateCapacityRequest updateCapacityRequest = new UpdateCapacityRequest(target);
        HttpResponse response = executePut(url, updateCapacityRequest);
        JsonObject data = parseResponse(response);

        System.out.println(data);

        JsonObject statusObject = data.get("response").getAsJsonObject().get("status").getAsJsonObject();
        int statusCode = statusObject.get("code").getAsInt();
        if (statusCode != 200) {
            return Optional.of(statusObject.get("message").getAsString());
        }

        return Optional.empty();
    }

    public Integer getInstancesCountForElasticGroup(String elasticGroupId) throws IOException {
        String url = String.format("https://api.spotinst.io/aws/ec2/group/%s/status?accountId=%s", elasticGroupId, ACCOUNT_ID);
        HttpResponse response = executeGet(url);
        JsonObject data = parseResponse(response);

        JsonObject statusObject = data.get("response").getAsJsonObject().get("status").getAsJsonObject();
        int statusCode = statusObject.get("code").getAsInt();
        if (statusCode != 200) {
            //logger.log("Error message:" + statusObject.get("message").getAsString())
            return -1;
        }

        return data.get("response").getAsJsonObject().get("count").getAsInt();
    }

    private HttpResponse executeGet(String url) throws IOException {
        return client.execute(new HttpGet(url));
    }

    private HttpResponse executePut(String url, Object body) throws IOException {
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity(gson.toJson(body)));
        return client.execute(httpPut);
    }


    private JsonObject parseResponse(HttpResponse response) throws IOException {
        return parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
    }
}
