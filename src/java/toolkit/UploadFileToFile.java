package toolkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
//OutputSream to File
public class UploadFileToFile {
    
    private static final Logger logger = Logger.getLogger(UploadFileToFile.class.getName());
    private  OutputStream out;
    
    public void store(File targetFile, InputStream inputStream) {
        
        //Zapisz plik na dysku we wskazanej lokalizacji (kartotece) 
        logger.log(Level.INFO, "UploadFileToFile / store: zapisz plik na dysku we wskazanej lokalizacji (kartotece)");
        try {
            logger.log(Level.INFO, "UploadFileToFile / store: targetFile path=" + targetFile.getPath());
            out = new FileOutputStream(targetFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            inputStream.close();
            out.flush();
            out.close();
            logger.log(Level.INFO, "UploadFileToFile / store: file " +targetFile.getName()+" written to "+targetFile.getPath());
        } catch (IOException e) { e.printStackTrace();
        }
    }
    
}
