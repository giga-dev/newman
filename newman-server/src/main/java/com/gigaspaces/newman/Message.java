package com.gigaspaces.newman;

public class Message {
    private String id;
    private Object content;

    public Message() {
    }

    public Message(String id, Object content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}