/**
 * 
 */
package experimentalcode.frankenb.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import experimentalcode.frankenb.log.Log;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class FileUtils {

  private FileUtils() {
  }

  public static void copy(File file, File toFile) throws IOException {
    Log.info("copying " + file + " to " + toFile + " ...");
    
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new FileInputStream(file);
      out = new FileOutputStream(toFile);

      int read = 0;
      byte[] buffer = new byte[2048];

      while((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }

    }
    finally {
      if(in != null)
        in.close();
      if(out != null)
        out.close();
    }

  }

}
