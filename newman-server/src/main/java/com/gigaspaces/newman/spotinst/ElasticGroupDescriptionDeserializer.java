package com.gigaspaces.newman.spotinst;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ElasticGroupDescriptionDeserializer implements JsonDeserializer<ElasticGroupDescription> {

    private Gson gson = new Gson();

    @Override
    public ElasticGroupDescription deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return gson.fromJson(jsonElement.getAsString(), ElasticGroupDescription.class);
    }
}
