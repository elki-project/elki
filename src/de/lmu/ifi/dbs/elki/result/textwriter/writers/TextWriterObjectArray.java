package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object into the inline section, using the objects toString method.
 * 
 * @author Erich Schubert
 * 
 */
public class TextWriterObjectArray<T> extends TextWriterWriterInterface<T[]> {
  /**
   * Serialize an object into the inline section.
   */
  @Override
  public void write(TextWriterStream out, String label, T[] v) {
    StringBuffer buf = new StringBuffer();
    if(label != null) {
      buf.append(label).append("=");
    }
    if(v != null) {
      for (T o : v) {
        buf.append(o.toString());
      }
    }
    out.inlinePrintNoQuotes(buf.toString());
  }
}
