package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object into the inline section, using the objects toString method.
 * 
 * @author Erich Schubert
 * 
 */
public class TextWriterVector extends TextWriterWriterInterface<Vector> {
  /**
   * Serialize an object into the inline section.
   */
  @Override
  public void write(TextWriterStream out, String label, Vector v) {
    String res = "";
    if(label != null) {
      res = res + label + "=";
    }
    if(v != null) {
      res = res + v.toStringNoWhitespace();
    }
    out.inlinePrintNoQuotes(res);
  }
}
