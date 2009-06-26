package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;

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

}
