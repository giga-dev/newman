package com.gigaspaces;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */
public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {
    @Override
    public void configure(WebSocketServletFactory factory){
        factory.register(EventSocket.class);
    }
}
