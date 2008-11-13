package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Write an object into the inline section, using the objects toString method.
 * 
 * @author Erich Schubert
 *
 */
public class TextWriterObjectInline extends TextWriterWriterInterface<Object> {
  @Override
  public void write(TextWriterStream out, String label, Object object) throws UnableToComplyException {
    String res = "";
    if (label != null) res = res+label+"=";
    if (object != null) res = res+object.toString();
    out.inlinePrintNoQuotes(res);
  }
}
