package toolkit;
/*
 * 1. JavaMail – GMail via TLS: 
 * 2. JavaMail – GMail via SSL
 * Outgoing Mail (SMTP) Server
 * requires TLS or SSL: smtp.gmail.com (use authentication)
 * Use Authentication: Yes
 * Port for TLS/STARTTLS: 587
 * Port for SSL: 465
 * To run this example, you need two dependency libraries – 
 * javaee.jar and mail.jar, both are bundle in JavaEE SDK.
 * GMail SMTP detail here – http://mail.google.com/support/bin/answer.py?hl=en&answer=13287
 * ---------------------------------------------------------
 * KLUCZOWA SPRAWA: if you are using javamail 1.4.2+, there is a socket factory 
 * you can use to ignore server certificate.
 * socketFactory = new MailSSLSocketFactory();
 * socketFactory.setTrustAllHosts(true);
 * props.put("mail.imaps.ssl.socketFactory", socketFactory);
 */

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.MailSSLSocketFactory;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import javax.mail.PasswordAuthentication;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Email {

    private Properties props;
    private MyAuthenticator authentication;
    private Session session;
    private MimeMessage message;
    private BodyPart messageBodyPart;
    private Transport transport;
    private MailSSLSocketFactory socketFactory;

    public void send(String recipeintEmail,
            String subject,
            String messageText,
            String[] attachments)
            throws MessagingException, AddressException {
        /*
        It is a good practice to put this in a java.util.Properties 
        file and encrypt password. Scroll down 
        to comments below to see 
        how to use java.util.Properties in JSF context. 
         */
        System.out.println("Email / send: recipeint=" + recipeintEmail);
        System.out.println("Email / send: subject=" + subject + " messageText=" + messageText);
        String senderEmail = "szpytko.michal@gmail.com"; //"your-gmail-account@gmail.com";
        String senderMailPassword = "mars180852"; //sender-account-password";
        String gmail = "smtp.gmail.com";
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        //trustStore=C:\glassfish3\glassfish\domains\domainMS2/config/cacerts.jks
        String keyStore = System.getProperty("javax.net.ssl.keyStore");
        //javax.net.ssl.keyStore=C:\glassfish3\glassfish\domains\domainMS2/config/keystore
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        //

        System.out.println("Email / send: trustStore=" + trustStore);
        System.out.println("Email / send: keyStore=" + keyStore);
        System.out.print("trustStorePassword=" + trustStorePassword);

        //System.setProperty("javax.net.ssl.trustStore", "cacerts.jks--new"); //cacerts.jks
        //System.setProperty("javax.net.ssl.trustStorePassword", "mars188key");  
        System.out.print("(new) trustStorePassword=" + System.getProperty("javax.net.ssl.trustStorePassword"));

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        try {
            props = System.getProperties();
            //if you are using javamail 1.4.2+, there is a socket factory 
            //you can use to ignore server certificate.
            socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            props.put("mail.imaps.ssl.socketFactory", socketFactory);

            props.put("mail.smtp.user", senderEmail);
            props.put("mail.smtp.host", "smtp.gmail.com");
            //props.put("mail.smtp.port", "587");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.debug", "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");

            props.put("mail.smtps.port", "465");
            props.put("mail.smtps.host", "smtp.gmail.com");
            props.put("mail.smtps.auth", "true");
            props.put("mail.smtps.ssl.checkserveridentity", "false");
            props.put("mail.smtps.ssl.trust", "*");
            //props.put("mail.smtps.quitwait", "false");
            props.put("mail.transport.protocol", "smtps");


            //System.out.println("Email / send: props=" + props);

            System.out.println("Email / send: senderMailPassword=" + senderMailPassword);
            // Required to avoid security exception.
            authentication =
                    new MyAuthenticator(senderEmail, senderMailPassword);
            System.out.println("Email / send: authentication=" + authentication.toString());

            session =
                    //Session.getDefaultInstance(props,authentication);
                    Session.getInstance(props, authentication);

            session.setDebug(true);

            System.out.println("Email / send: session=" + session.toString());

            message = new MimeMessage(session);

            System.out.println("Email / send: message subject=" + message.toString());

            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(messageText);

            // Add message text
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            //System.out.println("Email / send: multipart content type=" + multipart.getContentType());

            // Attachments should reside in your server.
            // Example "c:\file.txt" or "/home/user/photo.jpg"

            for (int i = 0; i < attachments.length; i++) {

                if (attachments[i] != null) {
                    messageBodyPart = new MimeBodyPart();
                    try {
                        //System.out.println("Email / send: attachments[" + i + "]=" + attachments[i]);
                        DataSource source = new FileDataSource(attachments[i]);
                        //System.out.println("Email / send: source=" + source.getName());
                        messageBodyPart.setDataHandler(new DataHandler(source));
                        messageBodyPart.setFileName(attachments[i]);
                        multipart.addBodyPart(messageBodyPart);
                    } catch (Exception ex) {
                        System.out.println("Exception ex=" + ex.getMessage());
                    }
                }
            }

            System.out.println("Email / send: message=" + message.toString());

            message.setContent(multipart);
            message.setSubject(subject);
            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(recipeintEmail));

            //transport = session.getTransport("smtp");
            //transport = session.getTransport("smtps");
            SMTPTransport transport = (SMTPTransport) session.getTransport("smtps");
            //System.out.println("Email / send: transport=" + transport.toString());
            //connect ...
            //transport.connect(gmail, 587, senderEmail, senderMailPassword);
            //transport.connect(gmail, 465, senderEmail, senderMailPassword);
            transport.connect();

            transport.sendMessage(message, message.getAllRecipients());

            transport.close();

            System.out.println("Email / send: I just send an e-mail - I emailed on the " + subject);

        } catch (Exception ex) {
            System.out.println("Email / send: ex=" + ex.getMessage());
        }
    }

    private class MyAuthenticator extends javax.mail.Authenticator {

        String User;
        String Password;

        public MyAuthenticator(String user, String password) {
            User = user;
            Password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new javax.mail.PasswordAuthentication(User, Password);
        }
    }
}
