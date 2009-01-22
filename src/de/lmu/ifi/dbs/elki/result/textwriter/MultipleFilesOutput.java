package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Manage output to multiple files.
 * 
 * @author Erich Schubert
 */
public class MultipleFilesOutput implements StreamFactory {
  /**
   * File name extension.
   */
  private final static String EXTENSION = ".txt";
  
  /**
   * Default stream to write to when no name is supplied.
   */
  private PrintStream defaultStream = null;
  /**
   * Base file name.
   */
  private File basename;
  /**
   * HashMap of open print streams.
   */
  private HashMap<String, PrintStream> map = new HashMap<String, PrintStream>();
  
  /**
   * Constructor
   * 
   * @param base Base file name (folder name)
   */
  public MultipleFilesOutput(File base) {
    basename = base;
  }

  /**
   * Retrieve/open the default output stream.
   * 
   * @return default output stream
   * @throws FileNotFoundException
   */
  private PrintStream getDefaultStream() throws FileNotFoundException {
    if (defaultStream != null)
      return defaultStream;
    defaultStream = newStream("default");
    return defaultStream;
  }
  
  /**
   * Open a new stream of the given name
   * 
   * @param filename file name (which will be appended to the base name)
   * @return stream object for the given name
   * @throws FileNotFoundException
   */
  private PrintStream newStream(String filename) throws FileNotFoundException {
    //System.err.println("Requested stream: "+filename);
    PrintStream res = map.get(filename);
    if (res != null) return res;
    // Ensure the directory exists:
    if (!basename.exists())
      basename.mkdirs();
    File n = new File(basename.getAbsolutePath()+File.separator+filename+EXTENSION);
    res = new PrintStream(new FileOutputStream(n));
    map.put(filename, res);
    return res;
  }
  
  /**
   * Retrieve the output stream for the given file name.
   */
  @Override
  public PrintStream openStream(String filename) throws FileNotFoundException {
    if (filename == null)
      return getDefaultStream();
    return newStream(filename);
  }
}
