package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface for output handling (single file, multiple files, ...)
 * 
 * @author Erich Schubert
 *
 */
public interface StreamFactory {
  /**
   * Retrieve a print stream for output using the given label.
   * Note that multiple labels MAY result in the same PrintStream, so you
   * should be printing to only one stream at a time to avoid mixing outputs.
   * 
   * @param label Output label.
   * @return stream object for the given label
   * @throws IOException on IO error
   */
  public PrintStream openStream(String label) throws IOException;
  
  /**
   * Close (and forget) all streams the factory has opened.
   */
  public void closeAllStreams();
}