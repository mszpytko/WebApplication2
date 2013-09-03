package fileservice;

//import com.mysql.jdbc.Connection;
//import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Connection;
import java.io.File;
import java.io.IOException;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.primefaces.component.menuitem.MenuItem;
import org.primefaces.component.submenu.Submenu;
import org.primefaces.model.DefaultMenuModel;
import org.primefaces.model.MenuModel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.InputStream;
import java.io.Serializable;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseEvent;
import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.context.RequestContext;
import toolkit.Email;
import toolkit.UploadFileToFile;
import utils.FacesUtil;
import utils.RefreshJSFPage;

//@ManagedBean
@ManagedBean(name = "fileController")
@RequestScoped
public class FileController implements Serializable {

    private static final Logger logger = Logger.getLogger(FileController.class.getName());
    private RefreshJSFPage refreshPage = new RefreshJSFPage();
    private MenuModel model;
    private MenuModel SWFModel;
    private MenuModel FLVModel;
    private MenuModel MP4Model;
    private MenuItem item;
    private String returnBack;
    private UploadedFile file;
    private FacesMessage msg;
    private FacesContext fctx;
    private ServletContext servletContext;
    private StreamedContent downloadFile;
    private File targetFolder;
    private File targetFile;
    private Connection connection;
    private PreparedStatement statement;
    private Email email;
    private String[] attachments = new String[1]; //UWAGA: na rozmiar: posylamy po 1 pliku!
    private FileNameMap fileNameMap;
    private String mimeType;
    private long filesize = 0;
    private String coreFileName;
    private String defaultSWFFile = "player_flv_mini.swf"; //player_flv_classiv.swf player_flv_multi.swf
    private String flashVars;
    private String newFileName;
    private String FLVFileName = "";//"irena-lektyka.flv";//"ZAZ - Je Veux.flv"; 
    private String SWFFileName = ""; //"/WebApplication2/photocam/makau3-kasyno.swf" 
    private String fileName = "eq-irena-lektyka.mp4";
    //NIE TAK - BLAD: "C:/TMP/images/eq-irena-lektyka.mp4";
    //NIE TAK - BLAD: "C:\\TMP\\images\\eq-irena-lektyka.mp4"; 

    public String getSWFFileName() {
        System.out.println("getSWFFileName: SWFFileName=" + SWFFileName);

        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        System.out.println("servletContext=" + servletContext.getContextPath());
        String absoluteDiskPath = servletContext.getRealPath("photocam" + "/" + SWFFileName);
        System.out.println("absoluteDiskPath=" + absoluteDiskPath);
        newFileName = servletContext.getContextPath() + "/photocam" + "/" + SWFFileName;
        File f = new File(absoluteDiskPath);
        if (!f.exists()) {
            newFileName = servletContext.getContextPath() + "/photocam" + "/" + defaultSWFFile;
        }
        System.out.println("FileController getFileName: newFileName=" + newFileName);
        return newFileName;
    }

    public void setSWFFileName(String SWFFileName) {
        this.SWFFileName = SWFFileName;
        //refreshPage.refresh();
        //refreshPage.response();
        System.out.println("setSWFFileName: SWFFileName=" + SWFFileName);
    }

    public String getFLVFileName() {
        System.out.println("getFLVFileName: FLVFileName=" + FLVFileName);
        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        //System.out.println("servletContext="+servletContext.getContextPath());
        newFileName = servletContext.getContextPath() + "/photocam" + "/" + FLVFileName;
        System.out.println("FileController getFLVFileName: newFileName=" + newFileName);
        return newFileName;

    }

    public String getFlashVars() {
        System.out.println("FileController getFlashVars: flv=" + getFLVFileName());
        return "flv=" + getFLVFileName();
    }

    public void setFLVFileName(String FLVFileName) {
        this.FLVFileName = FLVFileName;
        //refreshPage.refresh();
        //refreshPage.response();
        System.out.println("setFLVFileName: FLVFileName=" + FLVFileName);
    }

    public String getReturnBack() {
        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        System.out.println("getReturnBack: servletContextPath=" + servletContext.getContextPath());
        return (servletContext.getContextPath() + "/?faces-redirect=true");
    }

    public void setReturnBack(String returnBack) {
        this.returnBack = returnBack;
    }

    public FileController() {
        model = new DefaultMenuModel();
    }

    /*
     * Tworzymy dynamiczne menu, ktorego pozycje (item) stanowi wykaz nazw plikow w kartotece 
     * "photocam" naszej aplikacji. Po wybraniu ("select") pozycji menu, nazwa pliku ma byc przypisana
     * do wlasciwosci fileName, jako nazwa aktualnie wybranego pliku multimedialnego, ktory ma byc 
     * (po automatycznym odswierzeniu odpowiedniego panelu iezacej strony)  wyswietlony na stronie
     * Obsluga dynamicznie tworzonego modelu menu przy pomocy sluchcza akcji "select"
     * na pdst. http://forum.primefaces.org/viewtopic.php?f=3&t=7103
     * wykorzystuje MenuModel i MenuItem
     * import org.primefaces.component.menuitem.MenuItem;
     * import org.primefaces.component.submenu.Submenu;
     * import org.primefaces.model.DefaultMenuModel;
     * import org.primefaces.model.MenuModel;
     * patrz na metody MenuItem: addActionListener, broadcast, setAjax, setValue, ...
     *  item.setValue(files[i].getName());
     *  item.setAjax(false);
     *  item.addActionListener(FacesUtil.createMethodActionListener("#{fileController.menuSelectActionListener}", Void.class, new Class[]{ActionEvent.class}));
     * UWAGA:  powyższa operacja to dodanie do kolejnej pozycji, tj. MenuItem sluchcza akcji "select" - naszym przypadku, 
     * sluchaczem jest metoda naszej klasy (naszego ziarna fileController) o nazwie menuSelectActionListener (patrz dalej)
     * Patrz rowniez na nasza klase pomocnicza utils.FacesUtil
    
     */
    public MenuModel getModel() {

        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        if (model == null) {

            model = new DefaultMenuModel();
            //item = new MenuItem();
            //item.setValue("NOTHING");
            //model.addMenuItem(item);
        }

        try {

            targetFolder = new File(servletContext.getRealPath("") + File.separator + "photocam");
            File[] files = targetFolder.listFiles();
            for (int i = 0; i < files.length; i++) {

                item = new MenuItem();
                item.setValue(files[i].getName());
                item.setAjax(false);
                item.addActionListener(FacesUtil.createMethodActionListener("#{fileController.menuSelectActionListener}", Void.class, new Class[]{ActionEvent.class})); //SELECT action listener

                model.addMenuItem(item);
            }//i

        } catch (Exception e1) {
            System.out.println("Exception e1=" + e1.getMessage());
        }

        return model;
    }

    //tylko pliki SWF
    public MenuModel getSWFModel() {
        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        if (SWFModel == null) {
            SWFModel = new DefaultMenuModel();
            //item = new MenuItem();
            //item.setValue("NOTHING");
            //SWFModel.addMenuItem(item);
        }

        try {

            targetFolder = new File(servletContext.getRealPath("") + File.separator + "photocam");
            File[] files = targetFolder.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith("swf")) {

                    item = new MenuItem();
                    item.setValue(files[i].getName());
                    item.setAjax(false);
                    item.addActionListener(FacesUtil.createMethodActionListener("#{fileController.menuSelectActionListener}", Void.class, new Class[]{ActionEvent.class})); //SELECT action listener

                    SWFModel.addMenuItem(item);
                }//i
            }
        } catch (Exception e2) {
            System.out.println("Exception e2=" + e2.getMessage());
        }
        return SWFModel;
    }

    //tylko pliki FLY
    public MenuModel getFLVModel() {
        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        if (FLVModel == null) {
            FLVModel = new DefaultMenuModel();
        }

        try {

            targetFolder = new File(servletContext.getRealPath("") + File.separator + "photocam");
            File[] files = targetFolder.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith("flv")) {

                    item = new MenuItem();
                    item.setValue(files[i].getName());
                    item.setAjax(false);
                    item.addActionListener(FacesUtil.createMethodActionListener("#{fileController.menuSelectActionListener}", Void.class, new Class[]{ActionEvent.class})); //SELECT action listener

                    FLVModel.addMenuItem(item);
                }//i
            }
        } catch (Exception e3) {
            System.out.println("Exception e2=" + e3.getMessage());
        }
        return FLVModel;
    }
    //tylko pliki MP4

    public MenuModel getMP4Model() {
        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        if (MP4Model == null) {
            MP4Model = new DefaultMenuModel();
        }

        try {

            targetFolder = new File(servletContext.getRealPath("") + File.separator + "photocam");
            File[] files = targetFolder.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith("mp4")) {

                    item = new MenuItem();
                    item.setValue(files[i].getName());
                    item.setAjax(false);
                    item.addActionListener(FacesUtil.createMethodActionListener("#{fileController.menuSelectActionListener}", Void.class, new Class[]{ActionEvent.class})); //SELECT action listener

                    MP4Model.addMenuItem(item);
                }//i
            }
        } catch (Exception e2) {
            System.out.println("Exception e2=" + e2.getMessage());
        }
        return MP4Model;
    }

    public void processAction(ActionEvent event)
            throws AbortProcessingException {
    }

    //Sluchacz akcji "select" - wybor odpowiedniego MenuItem
    public void menuSelectActionListener(ActionEvent event) {

        MenuItem selecteditem = (MenuItem) event.getSource();
        System.out.println("FileController menuSelectActionListener: event source=" + event.getSource().toString());
        System.out.println("FileController menuSelectActionListener: event component id=" + event.getComponent().getId());
        System.out.println("FileController menuSelectActionListener: event component parent id=" + event.getComponent().getParent().getId());
        String selectedFileName = (String) selecteditem.getValue();
        String coreFileName = selectedFileName.substring(0, selectedFileName.indexOf('.'));
        System.out.println("FileController menuSelectActionListener: coreFileName=" + coreFileName);
        System.out.println("FileController menuSelectActionListener: event selecteditem value=" + selectedFileName);

        setSWFFileName((String) coreFileName + ".swf");
        setFLVFileName((String) coreFileName + ".flv");
        setFileName((String) selectedFileName);

        /* Update a JSF component from a JSF backing bean method
         * Using PrimeFaces specific API, use RequestContext#update()
         */
        System.out.println("FileController menuSelectActionListener: update mainform:moviepanel2:flashobject1");
        RequestContext.getCurrentInstance().update("mainform:moviepanel2:flashobject1");

    }

    //UWAGA: plik video (np. eq-irena-lektyka.mp4) mozna umiescic w kartotece aplikacji photocam 
    //a jako parametr src "odtwarzacza" na stronie JSF 2 przekazac /WebApplication2/photocam/eq-irena-lektyka.mp4
    public String getFileName() {

        servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        //System.out.println("servletContext="+servletContext.getContextPath());
        newFileName = servletContext.getContextPath() + "/photocam" + "/" + fileName;
        System.out.println("FileController getFileName: newFileName=" + newFileName);
        return newFileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UploadedFile getFile() {
        if (file != null) {
            System.out.println("FileController getFile: " + file.getFileName());
        }
        return file;
    }

    public void setFile(UploadedFile file) {
        System.out.println("FileControllersetFile: " + file.getFileName());
        this.file = file;
    }

    //Moje rozszerzenie -- to jest ActionListener - ALE NIE DLA p:fileUpload
    public void handleFileUpload() {
        if (file != null) {
            System.out.println("FileController handleFileUpload: file=" + file.getFileName());

            //MessageUtil.addInfoMessage("upload.successful", file.getFileName() + " is uploaded.");
            logger.log(Level.INFO, "upload.successful: " + file.getFileName() + " is uploaded.");
            System.out.println("upload.successful: " + file.getFileName() + " is uploaded.");


        } else {
            System.out.println("handleFileUpload: ActionListener activated!");
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        System.out.println("FileController handleFileUpload: event:" + event.getFile().getFileName());
        this.file = event.getFile();

        msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        fctx = FacesContext.getCurrentInstance();
        fctx.addMessage(null, msg);
        logger.log(Level.INFO, "upload.successful: " + file.getFileName() + " is uploaded.");
    }

    /*
     * FindUploadEvent servise:
     * 1. store selected file in database
     * 2. store selected file in server photocam directory
     * 3. mail selected file
     */
    public void handleSelectFile(FileUploadEvent event) {
        System.out.println("FileController handleSelectFile: event:" + event.getFile().getFileName());
        file = event.getFile();

        msg = new FacesMessage("File ", event.getFile().getFileName() + " is selected.");
        fctx = FacesContext.getCurrentInstance();
        fctx.addMessage(null, msg);
        logger.log(Level.INFO, "File: " + file.getFileName() + " is selected.");
        /*
         * Zapisz plik do bazy danych
         * 
         */
        storeMovie();
        
        /*
         * UWAGA: tutaj przekopiowac wskazany plik (obiekt) UploadedFile jako File 
         * do kartoteki servletContext.getRealPath("") + File.separator + "photocam" 
         * pod nazwa jak oryginal, ktora nastepnie przypisac do zmiennej fileName (nazwa dla "odtwarzacza")
         * <!-- UploadedFile -> InputStream -> FileOutputStream -> File -->
         */
        //Zapisz plik na dysku we wskazanej lokalizacji (kartotece)  - tutaj "photocam" w kartotece buid/web/photocam
        toolkit.UploadFileToFile store2File = new toolkit.UploadFileToFile();
        try {
            servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            targetFolder = new File(servletContext.getRealPath("") + File.separator + "photocam");
            targetFile = new File(targetFolder, file.getFileName());
            logger.log(Level.INFO, "targetFolder absolute path=" + targetFolder.getAbsolutePath() + " targetFile=" + targetFile.getName());
            logger.log(Level.INFO, "targetFile absolute path=" + targetFile.getAbsolutePath() + " targetFile path=" + targetFile.getPath());
            logger.log(Level.INFO, "targetFile=" + targetFile);
            InputStream inputStream = file.getInputstream();
            store2File.store(targetFile, inputStream); //stream -->file
            setFileName(file.getFileName());
            coreFileName = fileName.substring(0, fileName.indexOf('.'));
            setSWFFileName((String) coreFileName + ".swf");
            setFLVFileName((String) coreFileName + ".flv");

        } catch (IOException e) { e.printStackTrace();
        }
        /*
         * Wyslij plik do adresata poczty
         * 
         */
        attachments[0] = targetFile.toString();
        logger.log(Level.INFO, "ImageStoreBean / handleFileUpload: send e-mail about stored image=" + attachments[0]);
        try {
            email = new Email();
            email.send("szpytko.michal@gmail.com", "TEST", "image:" + attachments[0] + " stored in database", attachments);
        } catch (MessagingException ex1) {
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, "Problem with Email send: ", ex1);
        }
    }
    
    //Store movie (video file) in database
    public void storeMovie() {

        logger.log(Level.INFO, "FileController / storeMovie=" + file);
        
        if (file != null) {
            String fileName=file.getFileName();
            logger.log(Level.INFO, "FileController / storeMovie: fileName=" + fileName);

            fileNameMap = URLConnection.getFileNameMap(); //Loads filename map (a mimetable) from a data file
            
            logger.log(Level.INFO, "FileController / storeMovie: fileNameMap="+fileNameMap);
            mimeType = fileNameMap.getContentTypeFor(fileName);
            //UWAGA: moze jakos inaczej ustalic mimetype?
            if(mimeType==null) {
                mimeType=fileName.substring(fileName.indexOf('.')+1);
            }
            filesize = file.getSize();
            coreFileName = fileName.substring(0, fileName.indexOf('.'));
            logger.log(Level.INFO, "FileController / storeMovie: mimeType="+mimeType+" / filesize="+filesize+" /coreFileName="+coreFileName);
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/itcuties?user=root&password=mars188");
            connection.setAutoCommit(false); // Set autocommit to false to manage it by hand
            statement = connection.prepareStatement(
                    "INSERT INTO movies (file,mimetype,moviename,filename,size) VALUES (?, ?, ?, ?, ? )");
            statement.setBinaryStream(1, file.getInputstream());
            statement.setString(2, mimeType);
            statement.setString(3, coreFileName);
            statement.setString(4, file.getFileName());
            statement.setLong(5, filesize);
            statement.executeUpdate();  // insert data to the database
            connection.commit();	// when autocommit=false
            System.out.println("storeMovie: movie file="+file.getFileName()+" has been stored");
        } catch (Exception ex5) {
            ex5.printStackTrace();
            Logger.getLogger(ImageStoreBean.class.getName()).log(Level.SEVERE, "Exception ex5:" + ex5.getMessage());
            // Add error message
            FacesMessage errorMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Upload error", ex5.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, errorMsg);
        }
    }

    public StreamedContent getDownloadFile() {
        return downloadFile;
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        System.out.println("CacheControlPhaseListener / beforePhase: event faces context=" + event.getFacesContext());
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        // Stronger according to blog comment below that references HTTP spec
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        // some date in the past
        response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");
    }
}
