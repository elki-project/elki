package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object into the comments section, using the objects toString()
 * method.
 * 
 * @author Erich Schubert
 * 
 */
public class TextWriterObjectComment extends TextWriterWriterInterface<Object> {
  @Override
  public void write(TextWriterStream out, String label, Object object) {
    String res = "";
    if(label != null) {
      res = res + label + "=";
    }
    if(object != null) {
      res = res + object.toString();
    }
    out.commentPrintLn(res);
  }
}
