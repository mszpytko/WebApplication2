
/* Obsluga operacji "capture" - zapis obrazu z kamery we wskazanym pliku dyskowym
 * na podstawie:
 * http://stackoverflow.com/questions/9845674/primefaces-photocam-showing-captured-image
 *
 */
package fileservice;

import fileservice.model.Image; //z moimi rozszerzeniami
import java.io.File;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import javax.faces.FacesException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.imageio.stream.FileImageOutputStream;
import javax.servlet.ServletContext;
import org.primefaces.event.CaptureEvent;
import org.primefaces.model.StreamedContent;

@ManagedBean(name = "photoCamServiceBean")
@SessionScoped
public class PhotoCamServiceBean {

    private StreamedContent capturedImage;
	private FileImageOutputStream imageOutput;
	private ServletContext servletContext;
    private String username;
    
    private HashMap<Integer, Image> photos = new HashMap<Integer, Image>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public StreamedContent getCapturedImage() {
        if (capturedImage != null) {
            System.out.println("PhotoCamServiceBean: getCapturedImage captured image name=" + capturedImage.getName());
        }
        return capturedImage;
    }

    public void setCapturedImage(StreamedContent capturedImage) {
        if (capturedImage != null) {
            System.out.println("PhotoCamServiceBean: setCapturedImage captured image name=" + capturedImage.getName());
        }
        this.capturedImage = capturedImage;
    }

    public void onCapture(CaptureEvent captureEvent) {

        String photo = "tmpimage"; //getRandomImageName();   
        
        System.out.println("PhotoCamServiceBean: onCapture event component id="+captureEvent.getComponent().getId());
        System.out.println("PhotoCamServiceBean: onCapture source class name="+captureEvent.getSource().getClass().getName());
        byte[] data = captureEvent.getData();

        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String newFileName = servletContext.getRealPath("") + File.separator + "photocam" + File.separator + photo + ".png";
        System.out.println("onCapture: newFileName=" + newFileName);

        //Zapis strumienia captureEvent.getData() do wskazanego pliku dyskowego o nazwie tmpimage.png w kartotece photocam
        try {
            imageOutput = new FileImageOutputStream(new File(newFileName));
            imageOutput.write(data, 0, data.length);
            imageOutput.close();
        } catch (Exception e) {
            throw new FacesException("onCapture: error in writing captured image");
        }
        
    }
}
