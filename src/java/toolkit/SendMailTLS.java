package toolkit;
 
import java.util.Properties;
 /*
 To run this example, you need two dependency libraries  javaee.jar and mail.jar, both are bundle in JavaEE SDK.

Outgoing Mail (SMTP) Server
requires TLS or SSL: smtp.gmail.com (use authentication)
Use Authentication: Yes
Port for TLS/STARTTLS: 587
Port for SSL: 465

GMail SMTP detail here � http://mail.google.com/support/bin/answer.py hl=en&answer=13287
*/
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
 
public class SendMailTLS {
 
	public static void main(String[] args) {
 
		final String username = "szpytko.michal@gmail.com";
		final String password = "mars180852";
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
 
		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });
 
		try {
 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("szpytko.michal@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("szpytko.michal@gmail.com"));
			message.setSubject("Testing Subject");
			message.setText("Dear Mail Crawler,"
				+ "\n\n No spam to my email, please!");
 
			Transport.send(message);
 
			System.out.println("Done");
 
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}