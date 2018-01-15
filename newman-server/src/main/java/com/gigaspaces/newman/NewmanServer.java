package com.gigaspaces.newman;


import com.gigaspaces.newman.config.Config;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.*;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

public class NewmanServer {
    private static final Logger logger = LoggerFactory.getLogger(NewmanServer.class);
    private static final String REALM_PROPERTIES_PATH = "newman.server.realm-config-path";
    private static final String DEFAULT_REALM_PROPERTIES_PATH = "src/test/resources/realm.properties";
    private static final String WEB_FOLDER_PATH = "newman.server.web-folder-path";
    private static final String DEFAULT_WEB_FOLDER_PATH = "./web";
    private static final String KEYS_PATH = "newman.keys-folder-path";

    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        MorphiaLoggerFactory.registerLogger(SLF4JLoggerImplFactory.class);

        Config config = Config.fromArgs(args);

        Server server = new Server(8080);
        /* security */
        LoginService loginService = new HashLoginService("MyRealm",
                getNonEmptySystemProperty(REALM_PROPERTIES_PATH, DEFAULT_REALM_PROPERTIES_PATH));
        server.addBean(loginService);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);

        ConstraintMapping mapping = createConstraintMapping("auth", true, "/*", "user", "admin");
        ConstraintMapping allowResourcesMapping = createConstraintMapping("auth2", false, "/api/newman/resource/*");
        ConstraintMapping allowMetadataMapping = createConstraintMapping("auth3", false, "/api/newman/metadata/*");

        security.setConstraintMappings(Arrays.asList(new ConstraintMapping[] {
            mapping, allowMetadataMapping, allowResourcesMapping
        }));

        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        /* security */

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setInitParameter("config", config.asJSON());
        context.setContextPath("/");
        // security
        context.setSecurityHandler(security);

        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder("default", defaultServlet);
        String webPath = getNonEmptySystemProperty(WEB_FOLDER_PATH, DEFAULT_WEB_FOLDER_PATH);
        File webDir = new File(webPath);
        if (!webDir.exists()) {
            logger.info("File {} not found", webDir.getAbsolutePath());
            String webDirInJar = NewmanServer.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            if (webDirInJar.toLowerCase().endsWith(".jar")) {
                webPath = "jar:" +webDirInJar + "!/web";
            } else {
                logger.error("can't find webdir, either set web dir using system property {} or run newman with java -jar newman-server-1.0.jar", WEB_FOLDER_PATH);
                System.exit(1);
            }
        }
        logger.info("Using {} to serve static content", webPath);
        holderPwd.setInitParameter("resourceBase", webPath);
        holderPwd.setInitOrder(2);
        context.addServlet(holderPwd, "/*");

        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
        jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyServlet.setInitParameter("javax.ws.rs.Application", "com.gigaspaces.newman.NewmanApp");
        jerseyServlet.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
        jerseyServlet.setInitParameter("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
        jerseyServlet.setInitParameter("com.sun.jersey.config.feature.Trace", "true");

        jerseyServlet.setInitOrder(0);


        ServletHolder webSocketHolder = context.addServlet(WebSocketServlet.class, "/events/*");
        webSocketHolder.setInitOrder(1);


        try {
            Resource keyStoreResource = createKeystoreResource();
            SslContextFactory sslContextFactory = new SslContextFactory(false);
            sslContextFactory.setKeyStoreResource(keyStoreResource);
            sslContextFactory.setKeyStorePassword("password");
            sslContextFactory.setKeyManagerPassword("password");
            sslContextFactory.setTrustStoreResource(keyStoreResource);
            sslContextFactory.setTrustStorePassword("password");

            ServerConnector https = new ServerConnector(server, sslContextFactory);
            https.setName("https");
            https.setPort(8443);


            server.addConnector(https);


            server.setHandler(context);


            server.start();
            for (Connector connector : server.getConnectors()) {
                logger.info("connector: {}, protocols {}, transport {}", connector.getName(), connector.getProtocols(), connector.getTransport());
            }
            logger.info("server started!");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        } finally {
            server.destroy();
        }
    }

    private static ConstraintMapping createConstraintMapping(String name, boolean authenticate, String pathSpec, String... roles) {
        Constraint constraint = new Constraint();
        constraint.setName(name);
        constraint.setAuthenticate(authenticate);
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec(pathSpec);
        mapping.setConstraint(constraint);
        if (roles != null) {
            constraint.setRoles(roles);
        }
        return mapping;
    }

    private static Resource createKeystoreResource() throws MalformedURLException {
        String filePath = getNonEmptySystemProperty(KEYS_PATH, "./keys/server.keystore");
        if(new File(filePath).exists()){
            return Resource.newResource(new File(filePath));
        }
        String jar = NewmanServer.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
        if(jar.toLowerCase().endsWith(".jar")){
            return Resource.newResource("jar:" + jar + "!/keys/server.keystore");
        }
        throw new IllegalStateException("Missing keystore file " + new File(filePath).getAbsoluteFile());
    }
}