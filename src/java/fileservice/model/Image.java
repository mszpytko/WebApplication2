package fileservice.model;

import org.primefaces.model.StreamedContent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Image {

    private static final Logger logger = Logger.getLogger(Image.class.getName());
    private Integer id;
    private StreamedContent image;
    private String mimetype;
    private String filename;
    private int width;
    private int height;
    private Long size;

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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public Image() {
    }

    public Image(Integer id, StreamedContent image) {
        this.id = id;
        this.image = image;

        //logger.log(Level.INFO, "Image: id=" + id);
    }

    public Image(Integer id, StreamedContent image, String mimetype, String filename, int width, int height, long size) {
        this.id = id;
        this.image = image;
        this.mimetype = mimetype;
        this.filename = filename;
        this.height=height;
        this.width=width;
        this.size=size;

        //logger.log(Level.INFO, "Image: id=" + id + " width="+width+" height="+height+" mimetype=" + mimetype + " filename=" + filename);
    }

    public Integer getId() {
        //logger.log(Level.INFO, "Image / getId: id=" + id);
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public StreamedContent getImage() {
        return image;
    }

    public void setImage(StreamedContent image) {
        this.image = image;
    }

    public String getInfo() {
        return id.toString() + " " + filename + " " + mimetype;
    }
    public String toString() {
        return new String("id="+id+" width="+width+" height="+height+" size="+size+" mimetype="+mimetype+" filename="+filename);
    }
}
