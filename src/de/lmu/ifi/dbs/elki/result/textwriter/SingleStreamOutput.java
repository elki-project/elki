package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class to output all result data to a single stream (e.g. Stdout, single file)
 * 
 * @author Erich Schubert
 *
 */
public class SingleStreamOutput implements StreamFactory {
  /**
   * Output stream
   */
  private PrintStream stream;
  
  /**
   * Constructor using stdout.
   * @throws IOException 
   */
  public SingleStreamOutput() throws IOException  {
    this(FileDescriptor.out);
  }
  
  /**
   * Constructor using stdout
   * 
   * @param gzip Use gzip compression
   * @throws IOException 
   */
  public SingleStreamOutput(boolean gzip) throws IOException  {
    this(FileDescriptor.out, gzip);
  }
  
  /**
   * Constructor with given file name.
   * 
   * @param out filename
   * @throws FileNotFoundException
   */
  public SingleStreamOutput(File out) throws IOException {
    this(new FileOutputStream(out));
  }

  /**
   * Constructor with given file name.
   * 
   * @param out filename
   * @param gzip Use gzip compression
   * @throws FileNotFoundException
   */
  public SingleStreamOutput(File out, boolean gzip) throws IOException {
    this(new FileOutputStream(out), gzip);
  }

  /**
   * Constructor with given FileDescriptor
   * 
   * @param out file descriptor
   * @throws IOException 
   */
  public SingleStreamOutput(FileDescriptor out) throws IOException {
    this(new FileOutputStream(out));
  }
  
  /**
   * Constructor with given FileDescriptor
   * 
   * @param out file descriptor
   * @param gzip Use gzip compression
   * @throws IOException 
   */
  public SingleStreamOutput(FileDescriptor out, boolean gzip) throws IOException {
    this(new FileOutputStream(out), gzip);
  }
  
  /**
   * Constructor with given FileOutputStream.
   * 
   * @param out File output stream
   * @throws IOException 
   */
  public SingleStreamOutput(FileOutputStream out) throws IOException {
    this(out, false);
  }

  /**
   * Constructor with given FileOutputStream.
   * 
   * @param out File output stream
   * @throws IOException 
   */
  public SingleStreamOutput(FileOutputStream out, boolean gzip) throws IOException {
    OutputStream os = out;
    if (gzip) {
      // wrap into gzip stream.
      os = new GZIPOutputStream(os);
    }
    this.stream = new PrintStream(os);
  }

  /**
   * Return the objects shared print stream.
   * 
   * @param filename ignored filename for SingleStreamOutput, as the name suggests
   */
  @Override
  public PrintStream openStream(String filename) {
    return stream;
  }

  /**
   * Close output stream.
   */
  @Override
  public void closeAllStreams() {
    stream.close();
  }
}
