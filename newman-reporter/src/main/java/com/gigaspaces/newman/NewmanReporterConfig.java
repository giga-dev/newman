package com.gigaspaces.newman;

import java.util.Properties;

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

/**
 * Created by moran on 8/13/15.
 */
public class NewmanReporterConfig {

    private static final String NEWMAN_SERVER_HOST = "newman.server.host";
    private static final String DEFAULT_NEWMAN_SERVER_HOST = "localhost";

    private static final String NEWMAN_SERVER_PORT = "newman.server.port";
    private static final String DEFAULT_NEWMAN_SERVER_PORT = "8443";

    private static final String NEWMAN_SERVER_REST_USER = "newman.server.rest.user";
    private static final String DEFAULT_NEWMAN_SERVER_REST_USER = "root";

    private static final String NEWMAN_SERVER_REST_PASSWORD = "newman.server.rest.password";
    private static final String DEFAULT_NEWMAN_SERVER_REST_PW = "root";

    private static final String NEWMAN_MAIL_USER = "newman.mail.user";
    private static final String DEFAULT_NEWMAN_MAIL_USER = "newman@gigaspaces.com";

    private static final String NEWMAN_MAIL_PASSWORD = "newman.mail.password";
    private static final String DEFAULT_NEWMAN_MAIL_PASSWORD = "password";

    private static final String NEWMAN_MAIL_RECIPIENTS = "newman.mail.recipients";
    private static final String DEFAULT_NEWMAN_MAIL_RECIPIENTS = "rnd_xap@gigaspaces.com";


    private Properties properties;

    public NewmanReporterConfig() {
        properties = new Properties();
        properties.putIfAbsent(NEWMAN_SERVER_HOST, getNonEmptySystemProperty(NEWMAN_SERVER_HOST, DEFAULT_NEWMAN_SERVER_HOST));
        properties.putIfAbsent(NEWMAN_SERVER_PORT, getNonEmptySystemProperty(NEWMAN_SERVER_PORT, DEFAULT_NEWMAN_SERVER_PORT));
        properties.putIfAbsent(NEWMAN_SERVER_REST_USER, getNonEmptySystemProperty(NEWMAN_SERVER_REST_USER, DEFAULT_NEWMAN_SERVER_REST_USER));
        properties.putIfAbsent(NEWMAN_SERVER_REST_PASSWORD, getNonEmptySystemProperty(NEWMAN_SERVER_REST_PASSWORD, DEFAULT_NEWMAN_SERVER_REST_PW));

        properties.putIfAbsent(NEWMAN_MAIL_USER, getNonEmptySystemProperty(NEWMAN_MAIL_USER, DEFAULT_NEWMAN_MAIL_USER));
        properties.putIfAbsent(NEWMAN_MAIL_PASSWORD, getNonEmptySystemProperty(NEWMAN_MAIL_PASSWORD, DEFAULT_NEWMAN_MAIL_PASSWORD));
        properties.putIfAbsent(NEWMAN_MAIL_RECIPIENTS, getNonEmptySystemProperty(NEWMAN_MAIL_RECIPIENTS, DEFAULT_NEWMAN_MAIL_RECIPIENTS));
    }

    public String getNewmanServerHost() {return properties.getProperty(NEWMAN_SERVER_HOST);}

    public String getNewmanServerPort() { return properties.getProperty(NEWMAN_SERVER_PORT);}

    public String getNewmanServerRestUser() { return properties.getProperty(NEWMAN_SERVER_REST_USER);}

    public String getNewmanServerRestPassword() { return properties.getProperty(NEWMAN_SERVER_REST_PASSWORD);}

    public String getNewmanMailUser() { return properties.getProperty(NEWMAN_MAIL_USER);}

    public String getNewmanMailPassword() { return properties.getProperty(NEWMAN_MAIL_PASSWORD);}

    public String getNewmanMailRecipients() { return properties.getProperty(NEWMAN_MAIL_RECIPIENTS);}

    public Properties getProperties() {
        return properties;
    }
}
