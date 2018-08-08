package com.gigaspaces.newman;

import com.google.gson.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */
@WebSocket(maxTextMessageSize = 64 * 1024, maxIdleTime = 60000)
public class EventSocket implements WebSocketListener {
    private static final Logger logger = LoggerFactory.getLogger(EventSocket.class);
    private static Set<EventSocket> endpoints
            = new CopyOnWriteArraySet<>();
    private Session session;
    private static Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
            .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime())).create();

    protected static void broadcast(Message message)
            throws IOException {

        endpoints.forEach(endpoint -> {
            synchronized (endpoint) {
                endpoint.session.getRemote().sendStringByFuture(gson.toJson(message));
            }
        });
    }

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i1) {

    }

    @Override
    public void onWebSocketClose(int i, String str) {
        logger.info("Disconnected from " + session.getRemoteAddress() + " , error; " + str + " , i = " + i);
        endpoints.remove(this);


    }

    @Override
    public void onWebSocketConnect(Session session) {
        logger.info("Connect from " + session.getRemoteAddress());
        this.session = session;
        endpoints.add(this);
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.error("EventSocket got exception: ", throwable);
    }

    @Override
    public void onWebSocketText(String message) {
        if (session.isOpen()) {
            logger.info("Received " + message + " from " + session.getLocalAddress());
//            session.getRemote().sendString("You - " + message, null);
        }
    }
}