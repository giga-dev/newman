package com.gigaspaces;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        MorphiaLoggerFactory.registerLogger(SLF4JLoggerImplFactory.class);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");


        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder holderPwd = new ServletHolder("default", defaultServlet);
        holderPwd.setInitParameter("resourceBase", "./web");
        holderPwd.setInitOrder(2);
        context.addServlet(holderPwd, "/*");

        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
        jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyServlet.setInitParameter("javax.ws.rs.Application", "com.gigaspaces.MyApplication");

        jerseyServlet.setInitOrder(0);


        ServletHolder webSocketHolder = context.addServlet(WebSocketServlet.class, "/events/*");
        webSocketHolder.setInitOrder(1);


        Server server = new Server(8080);
        try {


//            SslContextFactory sslContextFactory = new SslContextFactory(false);


//
//            ServerConnector https = new ServerConnector(server, sslContextFactory);
//            https.setName("https");
//            https.setPort(8443);


//            server.addConnector(https);

            server.setHandler(context);

            server.start();
            for (Connector connector : server.getConnectors()) {
                logger.info("connector: {}, protocols {}, trasport {}", connector.getName(), connector.getProtocols(), connector.getTransport());
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