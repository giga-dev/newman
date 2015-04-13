package com.gigaspaces;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class EventSocket {
    private static final Logger logger = LoggerFactory.getLogger(EventSocket.class);

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connect from {}", session.getRemoteAddress());
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        if (session.isOpen()) {
            logger.info("Received [{}] from {}", message, session.getLocalAddress());
            session.getRemote().sendString(message, null);
        }
    }
}