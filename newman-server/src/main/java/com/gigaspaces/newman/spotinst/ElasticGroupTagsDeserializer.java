package com.gigaspaces.newman.spotinst;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ElasticGroupTagsDeserializer implements JsonDeserializer<ElasticGroupTags> {

    private String getKey(JsonElement element) {
        return element.getAsJsonObject().get("tagKey").getAsString();
    }

    private String getValue(JsonElement element) {
        return element.getAsJsonObject().get("tagValue").getAsString();
    }


    @Override
    public ElasticGroupTags deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonArray tags = jsonElement.getAsJsonObject().get("launchSpecification").getAsJsonObject().get("tags").getAsJsonArray();
        ElasticGroupTags elasticGroupTags = new ElasticGroupTags();

        tags.forEach(element -> {
            String key = getKey(element);
            String value = getValue(element);
            if (key.equals("Name")) {
                elasticGroupTags.setName(value);
            } else if (key.equals("Description")) {
                elasticGroupTags.setDescription(value);
            } else if (key.equals("Owner")) {
                elasticGroupTags.setOwner(value);
            }
        });

        return elasticGroupTags;

    }
}
