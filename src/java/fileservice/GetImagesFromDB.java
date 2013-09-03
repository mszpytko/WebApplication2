/*
 * 1. przygotowuje jako images = new HashMap<Integer, Image> 
 *     kolekcje wszystkich "obrazkow"  z bazy danych  - patrz: init(); i getFromDB(); 
 * 2. udostepnia na stronie JSF 2 jako StreamedContent zawartosc pliku wskazanego 
 *     parametrem image_id "obrazka" - metoda: StreamedContent getImage();
 */
package fileservice;

import com.mysql.jdbc.Blob;
import fileservice.model.Image; //z moimi rozszerzeniami
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;

@ManagedBean(name = "getImagesFromDB")
@RequestScoped
//@ViewScoped
//@SessionScoped
//@ApplicationScoped
public class GetImagesFromDB implements Serializable {

    private static final Logger logger = Logger.getLogger(GetImagesFromDB.class.getName());
    private HashMap<Integer, Image> images = new HashMap<Integer, Image>();
    private int minImageId = Integer.MAX_VALUE;
    private int maxImageId = -1;
    private Blob blob = null;
	private Connection conn;
	private PreparedStatement ps;
	private ResultSet rs;
    private int width = 0;
    

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    private int height = 0;
    private long filesize = 0;

    public GetImagesFromDB() {
       
    }

    @PostConstruct
    public void init() {

        logger.log(Level.INFO, "GetImagesFromDB / init:");
        try {
            minImageId = Integer.MAX_VALUE;
            maxImageId = -1;
            blob = null;
            getFromDB(); //pobierz zawartosc tablicy bazy danych
        } catch (ClassNotFoundException ex1) {
            logger.log(Level.INFO, "GetImagesFromDB / init: ex1=" + ex1.getMessage());
        } catch (SQLException ex2) {
            logger.log(Level.INFO, "GetImagesFromDB / init: ex2=" + ex2.getMessage());
        } catch (Exception ex3) {
            logger.log(Level.INFO, "GetImagesFromDB / init: ex3=" + ex3.getMessage());
        }
    }
    /*
     * http://stackoverflow.com/questions/11914085/convert-bytes-to-streamedcontent-fro-download-jsf2-0-primefaces3-3-1
     * 
     * 
     * http://forum.primefaces.org/viewtopic.php?f=3&t=24725
     * 
     * The <p:graphicImage> requires a special getter method. It will be invoked twice 
     * per generated image, each in a completely different HTTP request. The first HTTP 
     * request, which has requested the HTML result of a JSF page, will invoke the getter 
     * for the first time in order to generate the HTML <img> element with the right 
     * unique and auto-generated URL in the src attribute which contains information 
     * about which bean and getter exactly should be invoked whenever the webbrowser 
     * is about to request the image. Note that the getter does at this moment not need 
     * to return the image's contents. It would not be used in any way as that's not 
     * how HTML works (images are not "inlined" in HTML output, but they are instead 
     * requested separately).
     * Once the webbrowser retrieves the HTML result as HTTP response, it will parse 
     * the HTML source in order to present the result visually to the enduser. 
     * Once the webbrowser encounters an <img> element during parsing the HTML source, 
     * then it will send a brand new HTTP request on the URL as specified in its src 
     * attribute in order to download the content of that image and embed it 
     * in the visual presentation. This will invoke the getter method for the second 
     * time which in turn should return the actual image content.
     */

    //Pobierz wszystkie "obrazki" z bazy danych do  images = new HashMap<Integer, Image>
    public void getFromDB() throws ClassNotFoundException, SQLException {

        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://localhost/itcuties?user=root&password=mars188");
        conn.setAutoCommit(false);
        logger.log(Level.INFO, "GetImagesFromDB / getFromDB: connected!");

        ps = conn.prepareStatement("SELECT id, file, mimetype,filename,width, height,size  FROM files");
        rs = ps.executeQuery();
        while (rs.next()) {
            Integer id = rs.getInt(1);
            //logger.log(Level.INFO, "GetImagesFromDB: id=" + id);
            blob = (Blob) rs.getBlob("file");
            String mimetype = rs.getString(3);
            String filename = rs.getString(4);
            width = rs.getInt(5);
            height = rs.getInt(6);
            filesize = rs.getLong(7);
            int blobLength = (int) blob.length();
            //logger.log(Level.INFO, "GetImagesFromDB: id=" + id + " mimetype=" + mimetype + " filename=" + filename + " blobLength=" + blobLength);
            byte[] data = blob.getBytes(1, blobLength);
            blob.free(); //release the blob and free up memory. (since JDBC 4.0)       
            images.put(id, new Image(
                    id, new DefaultStreamedContent(new ByteArrayInputStream(data), mimetype, filename), mimetype, filename, width, height, filesize));

            if (id > maxImageId) {
                maxImageId = id;
            }
            if (id < minImageId) {
                minImageId = id;
            }
        }
        rs.close();
        ps.close();
        conn.commit();
        conn.close();

        //logger.log(Level.INFO, "GetImagesFromDB: images=" + images.toString());
        logger.log(Level.INFO, "GetImagesFromDB / getFromDB: minImageId=" + minImageId + " / maxImageId=" + maxImageId);
        showListOfImages();
    }

    public void showListOfImages() {

        //logger.log(Level.INFO, "showListOfImages ------------------------");
        Iterator iter = images.keySet().iterator();

        while (iter.hasNext()) {

            Integer key = (Integer) iter.next();
            Image image = images.get(key);
            Integer id = image.getId();
            width = image.getWidth();
            height = image.getHeight();
            filesize = image.getSize();
            StreamedContent content = image.getImage();

            String type = content.getContentType();
            String name = content.getName();
            String contentAsString = content.toString();
            logger.log(Level.INFO, "key=" + key.toString() + " id=" + id.toString() + " type=" + type + " name=" + name + " content=" + contentAsString);

        }
        logger.log(Level.INFO, "showListOfImages ------------------------");
    }

    public List<Image> getImageList() {
        logger.log(Level.INFO, "GetImagesFromDB / getImageList: images.size=" + images.size());
        logger.log(Level.INFO, "GetImagesFromDB / getImageList: images=" + images.toString());
        logger.log(Level.INFO, "GetImagesFromDB / getImageList: minImageId=" + getMinImageId() + " maxImageId=" + maxImageId);
        return new ArrayList<Image>(images.values());
    }


    public StreamedContent getImage() {

        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the view. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            String image_id = context.getExternalContext().getRequestParameterMap().get("image_id");
            //System.out.println("image_id: " + image_id);

            if (image_id == null) {
                logger.log(Level.INFO, "GetImagesFromDB / getImage: ATTENTION - get minImageId=" + getMinImageId());
                //You have to return something.
                //return images.get(1).getImage(); //if you return null here then it won't work!!!   
                return images.get(getMinImageId()).getImage();
            }
            logger.log(Level.INFO, "GetImagesFromDB / getImage: image_id=" + image_id);
            Image image = images.get(Integer.parseInt(image_id));
            width = image.getWidth();
            height = image.getHeight();
            //logger.log(Level.INFO, "GetImagesFromDB / getImage: id=" + image.getId() + " width=" + width + " height=" + height);
            //return images.get(Integer.parseInt(image_id)).getImage();
            return image.getImage();
        }
    }

    public void setMinImageId(int minImageId) {
        this.minImageId = minImageId;
    }

    public int getMinImageId() {
        return (this.minImageId);
    }

    public void setMaxImageId(int mmaxImageId) {
        this.maxImageId = maxImageId;
    }

    public int getMaxImageId() {
        return (this.maxImageId);
    }

    public boolean inRange(int image_id) {
        //logger.log(Level.INFO, "GetImagesFromDB / inRange: image_id=" + image_id + " / min=" + getMinImageId() + " max=" + getMaxImageId());
        return ((image_id >= getMinImageId()) && (image_id <= getMaxImageId()));
    }


}
