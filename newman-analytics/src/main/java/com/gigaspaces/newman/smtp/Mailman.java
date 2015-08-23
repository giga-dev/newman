package com.gigaspaces.newman.smtp;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * Created by moran on 8/11/15.
 */
public class Mailman {

    /*
     Constants to be used in properties file
     */
    public static final String MAIL_ACCOUNT_USERNAME = "mail.account.username";
    public static final String MAIL_ACCOUNT_PASSWORD = "mail.account.password";

    public static final String MAIL_MESSAGE_RECIPIENTS = "mail.message.recipients";

    private final String userAccount;
    private final String userPassword;

    public enum Format {TEXT, HTML};

    public Mailman(String userAccount, String userPassword) {
        this.userAccount = userAccount;
        this.userPassword = userPassword;
    }

    /**
     * Compose a MimeMessage using the parameters provided.
     *
     * @param to Email address of the receiver.
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @param format one of the valid email formats.
     * @throws MessagingException
     */
    public void compose(String to, String subject, String bodyText, Format format) throws MessagingException {

        Session session = Session.getInstance(new MailProperties(), null);
        Transport transport = session.getTransport();

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.addRecipients(Message.RecipientType.TO, to);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            if (format.equals(Format.TEXT)) {
                msg.setText(bodyText);
            } else if (format.equals(Format.HTML)) {
                msg.setContent(bodyText, "text/html; charset=ISO-8859-1");
            } else {
                throw new UnsupportedOperationException("unsupported email format " + format);
            }

            transport.connect();
            transport.send(msg, userAccount, userPassword);
        } finally {
            transport.close();
        }
    }
}
