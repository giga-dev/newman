package com.gigaspaces.newman.smtp.mailman;

import java.util.Properties;

/**
 * Created by moran on 8/11/15.
 */
public class MailProperties extends Properties{

    public MailProperties() {
        super();
        setProperty("mail.transport.protocol", "smtp");
        setProperty("mail.host", "smtp.gmail.com");
        setProperty("mail.smtp.port", "587");
        setProperty("mail.smtp.starttls.enable", "true");
        setProperty("mail.smtp.connectiontimeout", "10000"); //Socket connection timeout in ms
        setProperty("mail.smtp.timeout", "10000"); //Socket I/O timeout value in ms
    }

    public MailProperties setPassword(String password) {
        setProperty("newman.mail.password", password);
        return this;
    }

    public String getPassword() {
        return getProperty("newman.mail.password");
    }
}
