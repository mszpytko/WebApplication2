package toolkit;
 
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
 
public class SendMailSSL {
    
	    public void send(String recipeintEmail,
            String subject,
            String messageText,
            String[] attachments)
            throws MessagingException, AddressException {
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
 
		Session session = 
                        //Session.getDefaultInstance(props,
                        Session.getInstance(props,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("szpytko.michal@gmail.com","mars180852");
				}
			});
                System.out.println("SendMailSSL / send: session="+session);
 
		try {
 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("szpytko.michal@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("szpytko.michal@gmail.com"));
			message.setSubject("Testing Subject");
			message.setText("Dear Mail Crawler," +
					"\n\n No spam to my email, please!");
 
			Transport.send(message);
 
                        System.out.println("SendMailSSL / send: done!");
 
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}