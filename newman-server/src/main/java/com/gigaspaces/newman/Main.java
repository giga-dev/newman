package com.gigaspaces.newman;


import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Collections;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        MorphiaLoggerFactory.registerLogger(SLF4JLoggerImplFactory.class);

        Server server = new Server(8080);
        /* security */
        LoginService loginService = new HashLoginService("MyRealm",
                "newman-server/src/test/resources/realm.properties");
        server.addBean(loginService);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);

        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"user", "admin"});
//        constraint.setRoles(new String[]{"admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        /* security */

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        // security
        context.setSecurityHandler(security);

        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder("default", defaultServlet);
        holderPwd.setInitParameter("resourceBase", "./newman-server/web");
        holderPwd.setInitOrder(2);
        context.addServlet(holderPwd, "/*");

        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
        jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyServlet.setInitParameter("javax.ws.rs.Application", "com.gigaspaces.newman.NewmanApp");

        jerseyServlet.setInitOrder(0);


        ServletHolder webSocketHolder = context.addServlet(WebSocketServlet.class, "/events/*");
        webSocketHolder.setInitOrder(1);


        try {


            SslContextFactory sslContextFactory = new SslContextFactory(false);
            sslContextFactory.setKeyStorePath("./newman-server/keys/server.keystore");
            sslContextFactory.setKeyStorePassword("password");
            sslContextFactory.setKeyManagerPassword("password");
            sslContextFactory.setTrustStorePath("./newman-server/keys/server.keystore");
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
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        } finally {
            server.destroy();
        }
    }
}