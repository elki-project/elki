package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

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
   * Constructor
   */
  public SingleStreamOutput()  {
    this(FileDescriptor.out);
  }
  
  /**
   * Constructor with given file name.
   * 
   * @param out filename
   * @throws FileNotFoundException
   */
  public SingleStreamOutput(File out) throws FileNotFoundException {
    this(new FileOutputStream(out));
  }

  /**
   * Constructor with given FileDescriptor
   * 
   * @param out file descriptor
   */
  public SingleStreamOutput(FileDescriptor out)  {
    this(new FileOutputStream(out));
  }
  
  /**
   * Constructor with given FileOutputStream.
   * 
   * @param out File output stream
   */
  public SingleStreamOutput(FileOutputStream out) {
    this(new PrintStream(out));
  }

  /**
   * Constructor with given PrintStream
   * 
   * @param out PrintStream to use.
   */
  public SingleStreamOutput(PrintStream out) {
    stream = out;
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

}
