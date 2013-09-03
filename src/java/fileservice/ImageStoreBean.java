/*
 *  1. zapis wybranego graficznego pliku lokalnego jako BLOB do bazy danych - storeImage() 
 *  2. j.w. ze zmiana formatu - metody: resizeImage()
 *  3. zapis pliku j.w. we wskazanej lokalizacji na serwerze
 *  4. wyslanie w/w pliku do odbiorcy poczty GMail
 */
package fileservice;

import com.sun.mail.util.MailSSLSocketFactory;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import org.imgscalr.Scalr.*;  //sluzy do przeskalowania obrazka

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
//import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
//import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import javax.faces.event.ValueChangeEvent;

import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.faces.application.ViewHandler;
import javax.annotation.Resource;
import javax.faces.bean.SessionScoped;
//import javax.faces.component.UIViewRoot;
import javax.imageio.ImageIO;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import toolkit.Email;
import toolkit.UploadFileToFile;
//import toolkit.SendMailSSL;

@ManagedBean
//@RequestScoped
@SessionScoped
public class ImageStoreBean {

    @Resource(name = "mail/myMailSession")
    private Session mailSession;
    private static final Logger logger = Logger.getLogger(ImageStoreBean.class.getName());
    private Connection connection;
    private PreparedStatement statement;
    private InputStream inputStream;
    private InputStream ins;
    private ByteArrayOutputStream outs;
    private UploadedFile file;
    private File targetFolder;
    private File targetFile;
    private StreamedContent content;
    private BufferedImage bimg;
    private BufferedImage resizedimage;
    private BufferedImage resizedimage2;
    private FileNameMap fileNameMap;
    private String mimeType;
    private int targetwidth = 0;
    private long filesize = 0;
    private int width = 0;
    private int height = 0;
    private int resizedwidth = 0;
    private int resizedheight = 0;
    private int resizedwidth2 = 0;
    private int resizedheight2 = 0;
    private int targetheight = 0;
    private toolkit.UploadFileToFile store2File;
    private FacesContext context;
    private Email email;
    private String[] attachments = new String[1]; //UWAGA: na rozmiar: posylamy po 1 pliku!
    private MailSSLSocketFactory socketFactory;

    public int getTargetheight() {
        return targetheight;
    }

    public void setTargetheight(int targetheight) {
        this.targetheight = targetheight;
    }

    public int getTargetwidth() {
        return targetwidth;
    }

    public void setTargetwidth(int targetwidth) {
        this.targetwidth = targetwidth;
    }

    public static BufferedImage resizeImage(BufferedImage img, org.imgscalr.Scalr.Method method,
            org.imgscalr.Scalr.Mode mode, int targetWidth, int targetHeight, BufferedImageOp... ops) {
        // Create quickly, then smooth and brighten it.  
        //img = resize(img, Method.SPEED, 125, OP_ANTIALIAS, OP_BRIGHTER);   
        BufferedImage image = org.imgscalr.Scalr.resize(img, method, mode,
                targetWidth, targetHeight, ops);
        // Let's add a little border before we return result.  
        logger.log(Level.INFO, "ImageStoreBean / resizeImage: new targetWidth=" + targetWidth + " targetHeight=" + targetHeight);
        return org.imgscalr.Scalr.pad(image, 4);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int type, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, type);//set width and height of image
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return resizedImage;
    }

    // Store file in the database: it is possible to store in 3 type of size (original, RESIZED i RESIZED2)
    public void storeImage() {

        logger.log(Level.INFO, "ImageStoreBean / storeImage: file=" + file);

        if (file != null) {
            logger.log(Level.INFO, "ImageStoreBean / storeImage: file=" + file.getFileName());
        }

        if ((targetFile == null) || (targetFolder == null)) {
            targetFolder = new File("C:/TMP/images");
            targetFile = new File(targetFolder, file.getFileName());
        }

        resizedimage = null;
        resizedimage2 = null;

        try {

            if (targetFile != null) {
                bimg = ImageIO.read(targetFile);
                width = bimg.getWidth();
                height = bimg.getHeight();
                filesize = targetFile.length();

                logger.log(Level.INFO, "ImageStoreBean / storeImage: original width=" + width + " height=" + height + " size=" + filesize);

                targetheight = 100;
                targetwidth = 200;
                int type = bimg.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bimg.getType();
                resizedimage = resizeImage(bimg, type, targetwidth, targetheight);
                resizedwidth = resizedimage.getWidth();
                resizedheight = resizedimage.getHeight();

                logger.log(Level.INFO, "ImageStoreBean / storeImage: resizedimage resizedwidth=" + resizedimage.getWidth()
                        + " resizedheight=" + resizedimage.getHeight() + " / type=" + type);

                //przeskalowanie z zachowaniem proporcji
                resizedimage2 = resizeImage(bimg, org.imgscalr.Scalr.Method.SPEED,
                        org.imgscalr.Scalr.Mode.FIT_TO_WIDTH, targetwidth, targetheight, org.imgscalr.Scalr.OP_ANTIALIAS);
                resizedwidth2 = resizedimage.getWidth();
                resizedheight2 = resizedimage.getHeight();
                logger.log(Level.INFO, "ImageStoreBean / storeImage: resizedimage2 resizedwidth2=" + resizedimage2.getWidth()
                        + " resizedheight2=" + resizedimage2.getHeight());
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, "Exception ex1:" + ex1.getMessage());
        }

        try {
            // Create connection
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost/itcuties?user=root&password=mars188");
            connection.setAutoCommit(false); // Set autocommit to false to manage it by hand
            // Create the statement object
            statement = connection.prepareStatement(
                    "INSERT INTO files (file,mimetype,filename,width,height,size) VALUES (?, ?, ?, ?, ?, ? )");
            // Set file data
            statement.setBinaryStream(1, file.getInputstream());
            statement.setString(2, mimeType);
            statement.setString(3, file.getFileName());
            statement.setInt(4, width);
            statement.setInt(5, height);
            statement.setLong(6, filesize);
            statement.executeUpdate();  // insert data to the database
            // Commit & close
            connection.commit();	// when autocommit=false

            //-----------------------------------------------
            // Store resized image
            // BufferedImage to ByteArrayInputStream
            if (resizedimage != null) {
                outs = new ByteArrayOutputStream();
                ImageIO.write(resizedimage, "jpg", outs);
                ins = new ByteArrayInputStream(outs.toByteArray());

                statement.setBinaryStream(1, ins);
                statement.setString(2, mimeType);
                statement.setString(3, file.getFileName() + "_RESIZED");
                statement.setInt(4, resizedwidth);
                statement.setInt(5, resizedheight);
                statement.setLong(6, filesize);
                // Insert data to the database
                statement.executeUpdate();
                // Commit & close
                connection.commit();
            }
            //---------------------------------------------------------
            if (resizedimage2 != null) {
                outs = new ByteArrayOutputStream();
                ImageIO.write(resizedimage2, "jpg", outs);
                ins = new ByteArrayInputStream(outs.toByteArray());

                statement.setBinaryStream(1, ins);
                statement.setString(2, mimeType);
                statement.setString(3, file.getFileName() + "_RESIZED2");
                statement.setInt(4, resizedwidth2);
                statement.setInt(5, resizedheight2);
                statement.setLong(6, filesize);
                statement.executeUpdate();
                connection.commit();

                connection.close();
            }

            logger.log(Level.INFO, "STORED FILE=" + file.getFileName());
        } catch (Exception ex2) {
            ex2.printStackTrace();
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, "Exception ex2:" + ex2.getMessage());
            // Add error message
            FacesMessage errorMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Upload error", ex2.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, errorMsg);
        }

    }

    // Getter method
    public UploadedFile getFile() {

        if (file != null) {
            logger.log(Level.INFO, "ImageStoreBean / setFile: file name=" + file.getFileName());
            logger.log(Level.INFO, "ImageStoreBean / setFile: file=" + file.toString());
        }
        return file;
    }

    // Setter method
    public void setFile(UploadedFile file) {

        logger.log(Level.INFO, "ImageStoreBean / setFile: file name=" + file.getFileName());
        logger.log(Level.INFO, "ImageStoreBean / setFile: file=" + file.toString());
        this.file = file;
    }

    public void handleFileUpload(FileUploadEvent event) {

        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: event=" + event.toString());
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: event filename=" + event.getFile().getFileName());
        file = event.getFile();
        fileNameMap = URLConnection.getFileNameMap();
        mimeType = fileNameMap.getContentTypeFor(event.getFile().getFileName());
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: mimeType=" + mimeType);
        setFile(file);
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: file=" + getFile());

        //FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        //FacesContext.getCurrentInstance().addMessage(null, msg);

        //Zapisz plik na dysku we wskazanej lokalizacji (kartotece) 
        store2File = new toolkit.UploadFileToFile();

        try {
            targetFolder = new File("C:/TMP/images");
            targetFile = new File(targetFolder, event.getFile().getFileName());
            inputStream = event.getFile().getInputstream();
            //toolkit.UploadFileToFile.store(File targetFile, InputStream inputStream)
            store2File.store(targetFile, inputStream);

        } catch (IOException e) { e.printStackTrace();
        }

        storeImage(); //Zapisz plik do bazy danych

        attachments[0] = targetFile.toString();
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: send e-mail about stored image=" + attachments[0]);
        try {
            /*
             * UWAGA: dla jak poniżej mamy problem:
             * Could not connect to SMTP host: smtp.gmail.com, port: 465
             * sendMessage("szpytko.michal@gmail.com", "TEST: as GF GMail service", "image:" + attachments[0] + " stored in database", attachments);
             */

            System.out.println("ImageStoreBean / handleFileUpload: email ...");

            email = new Email();
            email.send("szpytko.michal@gmail.com", "TEST", "image:" + attachments[0] + " stored in database", attachments);
        } catch (MessagingException ex1) {
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, "Problem with Email send: ", ex1);
        }


        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: EXIT");  //tyle razy, ile plikow wybrano!

        context = FacesContext.getCurrentInstance();
        String viewId = context.getViewRoot().getViewId();
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: viewId=" + viewId);
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: KONIEC");  //tyle razy, ile pliko
    }

    public StreamedContent getContent() throws FileNotFoundException {
        logger.log(Level.INFO, "ImageStoreBean / getContent: targetFile=" + targetFile);
        logger.log(Level.INFO, "ImageStoreBean / getContent: mimeType=" + mimeType);

        if (targetFile == null) {
            return null;
        }
        return new DefaultStreamedContent(new FileInputStream(targetFile), mimeType);
    }

    public void valueChangeListener(ValueChangeEvent event) {
        logger.log(Level.INFO, "ImageStoreBean / valueChangeListener: ...");
        storeImage();
    }

    //UWAGA: NIE MOZEMY UZYWAC TUTAJ W TYM MB HttpServletResponse, gdyz ten MB 
    //NIE JEST servlet'em!
    public void downloadImage() {
        try {
            //File file = new File("C:\\TMP\\images\\" + targetFile.getName());  //s-irena-klatka.jpg
            File file = new File("C:\\TMP\\images\\s-irena-klatka.jpg");
            InputStream fis = new FileInputStream(file);
            byte[] buf = new byte[(int) file.length()];
            int offset = 0;
            int numRead = 0;
            while ((offset < buf.length)
                    && ((numRead = fis.read(buf, offset, buf.length - offset)) >= 0)) {
                offset += numRead;
            }
            fis.close();

            logger.log(Level.INFO, "ImageStoreBean / downloadImage: file=" + file.getPath());

            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            logger.log(Level.INFO, "ImageStoreBean / downloadImage: response=" + response.toString());
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + file.getName() + "");
            response.getOutputStream().write(buf);
            response.getOutputStream().flush();
            response.getOutputStream().close();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException ex3) {
            System.out.println("Error : " + ex3);
        }
    }

    /*
     * UWAGA: 1. to jest próba wykorzystania: @Resource(name = "mail/myMailSession")
     * zdef. w GF
     * 2. NARAZIE NIE WYKORZYSTUJĘ !
     * 
     * UWAGA: wciaz mamy me=Could not connect to SMTP host: smtp.gmail.com, port: 465
     */
    public void sendMessage(String recipeintEmail,
            String subject,
            String messageText,
            String[] attachments) {

        //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider()); //NIC NIE DAJE?

        Message msg = new MimeMessage(mailSession);

        try {
            socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            System.out.println("*** GF *** (1) ImageStoreBean / sendMessage: msg content type=" + msg.getContentType());
            msg.setSubject(subject);
            msg.setRecipient(RecipientType.TO,
                    new InternetAddress(recipeintEmail));
            msg.setText(messageText);
            System.out.println("*** GF *** (2) ImageStoreBean / sendMessage: msg size=" + msg.getSize());
            Transport.send(msg);
            System.out.println("*** GF *** (3) ImageStoreBean / sendMessage: send ...");
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessagingException me) {
            System.out.println("*** GF *** (4) me=" + me.getMessage());
        }
    }
}
