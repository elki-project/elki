package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object, using the objects own {@link TextWriteable} interface.
 * 
 * @author Erich Schubert
 *
 */
public class TextWriterTextWriteable extends TextWriterWriterInterface<TextWriteable> {
  @Override
  public void write(TextWriterStream out, String label, TextWriteable obj) {
    obj.writeToText(out, label);
  }
}
