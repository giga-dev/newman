package com.gigaspaces.newman;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */
public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {
    @Override
    public void configure(WebSocketServletFactory factory){
        factory.getPolicy().setIdleTimeout(10000);
        factory.register(EventSocket.class);
    }
}
