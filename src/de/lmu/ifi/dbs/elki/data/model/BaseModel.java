package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Abstract base class for Cluster Models.
 * 
 * @author Erich Schubert
 *
 */
public abstract class BaseModel implements Model {
  /**
   * Implement writeToText as per {@link TextWriteable} interface.
   * However BaseModel is not given the interface directly, since
   * it is meant as signal to make Models printable. 
   * 
   * @param out
   */
  //@Override
  public void writeToText(TextWriterStream out) {
    out.commentPrintLn("Model class: "+this.getClass().getName());
  }
}
