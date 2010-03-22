package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

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
   * @param file File object
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
   * @param name File name
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

  /**
   * Try to open a stream as gzip, if it starts with the gzip magic.
   * 
   * TODO: move to utils package.
   * 
   * @param in original input stream
   * @return old input stream or a {@link GZIPInputStream} if appropriate.
   * @throws IOException
   */
  public static InputStream tryGzipInput(InputStream in) throws IOException {
    // try autodetecting gzip compression.
    if (!in.markSupported()) {
      PushbackInputStream pb = new PushbackInputStream(in, 16);
      in = pb;
      // read a magic from the file header
      byte[] magic = {0, 0};
      pb.read(magic);
      pb.unread(magic);
      if (magic[0] == 31 && magic[1] == -117) {
        in = new GZIPInputStream(pb);
      }
    } else
    if (in.markSupported()) {
      in.mark(16);
      if (in.read() == 31 && in.read() == -117) {
        in.reset();
        in = new GZIPInputStream(in);
      } else {
        // just rewind the stream
        in.reset();
      }
    }
    return in;
  }

  /**
   * Try to locate an file in the filesystem, given a partial name and a prefix.
   * 
   * @param name file name
   * @param basedir extra base directory to try
   * @return file, if the file could be found. {@code null} otherwise
   */
  public static File locateFile(String name, String basedir) {
    // Try exact match first.
    File f = new File(name);
    if(f.exists()) {
      return f;
    }
    // Try with base directory
    if(basedir != null) {
      f = new File(basedir, name);
      // logger.warning("Trying: "+f.getAbsolutePath());
      if(f.exists()) {
        return f;
      }
    }
    // try stripping whitespace
    {
      String name2 = name.trim();
      if(!name.equals(name2)) {
        // logger.warning("Trying without whitespace.");
        f = locateFile(name2, basedir);
        if(f != null) {
          return f;
        }
      }
    }
    // try substituting path separators
    {
      String name2 = name.replace('/',File.separatorChar);
      if (!name.equals(name2)) {
        // logger.warning("Trying with replaced separators.");
        f = locateFile(name2, basedir);
        if(f != null) {
          return f;
        }
      }
      name2 = name.replace('\\',File.separatorChar);
      if (!name.equals(name2)) {
        // logger.warning("Trying with replaced separators.");
        f = locateFile(name2, basedir);
        if(f != null) {
          return f;
        }
      }
    }
    // try stripping extra characters, such as quotes.
    if(name.length() > 2 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
      // logger.warning("Trying without quotes.");
      f = locateFile(name.substring(1, name.length() - 1), basedir);
      if(f != null) {
        return f;
      }
    }
    return null;
  }
}
