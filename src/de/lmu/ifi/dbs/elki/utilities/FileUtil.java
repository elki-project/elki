package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Various static helper methods to deal with files and file names.
 * 
 * @author Erich Schubert
 */
public final class FileUtil {
  /**
   * Returns the lower case extension of the selected file.
   * 
   * If no file is selected, <code>null</code> is returned.
   * 
   * @return Returns the extension of the selected file in lower case or
   *         <code>null</code>
   */
  public static String getFilenameExtension(File file) {
    return getFilenameExtension(file.getName());
  }

  /**
   * Returns the lower case extension of the selected file.
   * 
   * If no file is selected, <code>null</code> is returned.
   * 
   * @return Returns the extension of the selected file in lower case or
   *         <code>null</code>
   */
  public static String getFilenameExtension(String name) {
    if(name == null) {
      return null;
    }
    int index = name.lastIndexOf(".");
    if(index >= name.length() - 1) {
      return null;
    }
    return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
  }

  /**
   * Try to open a file, first trying the file system,
   * then falling back to the classpath.
   * 
   * @param filename File name in system notation
   * @return Input stream
   * @throws FileNotFoundException When no file was found.
   */
  public static InputStream openSystemFile(String filename) throws FileNotFoundException {
    try {
      return new FileInputStream(filename);
    }
    catch(FileNotFoundException e) {
      // try with classloader
      String resname = filename.replace(File.separatorChar, '/');
      InputStream result = ClassLoader.getSystemResourceAsStream(resname);
      if (result == null) {
        throw e;
      }
      return result;
    }
  }
}
