package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Base class for object writers.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public abstract class TextWriterWriterInterface<O> {
  /**
   * Write a given object to the output stream.
   * 
   * @param out
   * @param label
   * @param object
   * @throws UnableToComplyException
   * @throws IOException
   */
  public abstract void write(TextWriterStream out, String label, O object) throws UnableToComplyException, IOException;
  
  /**
   * Non-type-checking version.
   * 
   * @param out
   * @param label
   * @param object
   * @throws UnableToComplyException
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public final void writeObject(TextWriterStream out, String label, Object object) throws UnableToComplyException, IOException {
    write(out, label, (O) object);
  }
}