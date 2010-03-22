package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write a database object to the inline part (de-normalizing, no quoting)
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class TextWriterDatabaseObjectInline<O extends DatabaseObject> extends TextWriterWriterInterface<O> {
  /**
   * @param label label parameter ignored for this writer. 
   */
  @Override
  public void write(TextWriterStream out, String label, O object) {
    O restored = out.normalizationRestore(object);
    out.inlinePrintNoQuotes(restored.toString());
  }
}
