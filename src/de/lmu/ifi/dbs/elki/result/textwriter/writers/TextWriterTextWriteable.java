package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Write an object, using the objects own {@link TextWriteable} interface.
 * 
 * @author Erich Schubert
 *
 */
public class TextWriterTextWriteable extends TextWriterWriterInterface<TextWriteable> {
  @Override
  public void write(TextWriterStream out, String label, TextWriteable obj) throws UnableToComplyException, IOException {
    obj.writeToText(out);
  }
}
