package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.FileNotFoundException;
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
   * @return
   * @throws FileNotFoundException
   */
  public PrintStream openStream(String label) throws FileNotFoundException;
}
