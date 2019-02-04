/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Various static helper methods to deal with files and file names.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class FileUtil {
  /**
   * Fake Constructor. Use static methods.
   * 
   */
  private FileUtil() {
    // Do not instantiate.
  }

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
    int index = name.lastIndexOf('.');
    return index < 0 ? null : name.substring(index + 1).toLowerCase();
  }

  /**
   * Try to open a file, first trying the file system, then falling back to the
   * classpath.
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
      String resname = File.separatorChar != '/' ? filename.replace(File.separatorChar, '/') : filename;
      ClassLoader cl = FileUtil.class.getClassLoader();
      InputStream result = cl.getResourceAsStream(resname);
      if(result != null) {
        return result;
      }
      // Sometimes, URLClassLoader does not work right. Try harder:
      URL u = cl.getResource(resname);
      if(u == null) {
        throw e;
      }
      try {
        URLConnection conn = u.openConnection();
        conn.setUseCaches(false);
        if((result = conn.getInputStream()) != null) {
          return result;
        }
      }
      catch(IOException x) {
        throw e; // Throw original error instead.
      }
      throw e;
    }
  }

  /**
   * Try to open a stream as gzip, if it starts with the gzip magic.
   * 
   * @param in original input stream
   * @return old input stream or a {@link GZIPInputStream} if appropriate.
   * @throws IOException on IO error
   */
  public static InputStream tryGzipInput(InputStream in) throws IOException {
    // try autodetecting gzip compression.
    if(!in.markSupported()) {
      PushbackInputStream pb = new PushbackInputStream(in, 16);
      // read a magic from the file header, and push it back
      byte[] magic = { 0, 0 };
      int r = pb.read(magic);
      pb.unread(magic, 0, r);
      return (magic[0] == 31 && magic[1] == -117) ? new GZIPInputStream(pb) : pb;
    }
    // Mark is supported.
    in.mark(16);
    boolean isgzip = ((in.read() << 8) | in.read()) == GZIPInputStream.GZIP_MAGIC;
    in.reset(); // Rewind
    return isgzip ? new GZIPInputStream(in) : in;
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
      if((f = new File(basedir, name)).exists()) {
        return f;
      }
    }
    // try stripping whitespace
    String name2;
    if(!name.equals(name2 = name.trim())) {
      if((f = locateFile(name2, basedir)) != null) {
        return f;
      }
    }
    // try substituting path separators
    if(!name.equals(name2 = name.replace('/', File.separatorChar))) {
      if((f = locateFile(name2, basedir)) != null) {
        return f;
      }
    }
    if(!name.equals(name2 = name.replace('\\', File.separatorChar))) {
      if((f = locateFile(name2, basedir)) != null) {
        return f;
      }
    }
    // try stripping extra characters, such as quotes.
    if(name.length() > 2 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
      if((f = locateFile(name.substring(1, name.length() - 1), basedir)) != null) {
        return f;
      }
    }
    return null;
  }

  /**
   * Load an input stream (e.g., a Java resource) into a String buffer. The
   * stream is closed afterwards.
   * 
   * @param is Input stream
   * @return String with file/resource contents.
   * @throws IOException on IO errors
   */
  public static String slurp(InputStream is) throws IOException {
    StringBuilder buf = new StringBuilder();
    final byte[] b = new byte[4096];
    for(int n; (n = is.read(b)) != -1;) {
      buf.append(new String(b, 0, n));
    }
    is.close();
    return buf.toString();
  }
}
