package com.gigaspaces.newman.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.NewmanApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Barak Bar Orion
 * 4/29/15.
 */
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(NewmanApp.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private Mongo mongo;

    public Config() {
        this.mongo = new Mongo();
    }


    public Mongo getMongo() {
        return mongo;
    }

    public void setMongo(Mongo mongo) {
        this.mongo = mongo;
    }

    @Override
    public String toString() {
        try {
            return asJSON();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String asJSON() throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public static Config fromArgs(String[] args) {
        try {
            if (args != null && 0 < args.length) {
                File file = new File(args[0]);
                if (file.exists()) {
                    return fromFile(file);
                }
            }
        } catch (Exception e) {
            logger.error("error while trying to get configuration using args {}", Arrays.asList(args), e);
            Config res = new Config();
            logger.warn("Using default config {}", res);
        }
        Config res = new Config();
        logger.info("Using default config {}", res);
        return res;

    }

    private static Config fromFile(File file) throws IOException {
        return mapper.readValue(file, Config.class);
    }


    public static Config fromString(String config){
        try {
            return mapper.readValue(config, Config.class);
        }catch(Exception e){
            Config res = new Config();
            logger.warn("Error while reading config from string '{}' using default configuration {}", config, res, e);
            return res;
        }
    }
}
