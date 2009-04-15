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
   * @param label 
   */
  //@Override
  public void writeToText(TextWriterStream out, String label) {
    if (label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(TextWriterStream.SER_MARKER+" " + BaseModel.class.getName());
  }
}
