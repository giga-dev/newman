package com.gigaspaces.newman.smtp.mailman;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * Created by moran on 8/11/15.
 */
public class Mailman {

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param from Email address of the sender, the mailbox account.
     * @param to Email address of the receiver.
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @throws MessagingException
     */
    public static void createTextEmail(MailProperties props, String from, String to, String subject,
                                       String bodyText) throws MessagingException {

        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport();

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(from);
            msg.addRecipients(Message.RecipientType.TO, to);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            msg.setText(bodyText);

            transport.connect();
            transport.send(msg, from, props.getPassword());
        } finally {
            transport.close();
        }
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param from Email address of the sender, the mailbox account.
     * @param to Email address of the receiver.
     * @param subject Subject of the email.
     * @param content Html content of the email.
     * @throws MessagingException
     */
    public static void createHtmlEmail(MailProperties props, String from, String to, String subject,
                                       String content) throws MessagingException {

        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport();

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(from);
            msg.addRecipients(Message.RecipientType.TO, to);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            msg.setContent(content, "text/html; charset=ISO-8859-1");

            transport.connect();
            transport.send(msg, from, props.getPassword());
        } finally {
            transport.close();
        }
    }
}
