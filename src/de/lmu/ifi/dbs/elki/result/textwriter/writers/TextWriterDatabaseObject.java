package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Write a database object to the inline part (de-normalizing, no quoting)
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public class TextWriterDatabaseObject<O extends DatabaseObject> extends TextWriterWriterInterface<O> {
  @Override
  public void write(TextWriterStream out, String label, O object) throws UnableToComplyException {
    try {
      O restored = out.normalizationRestore(object);
      out.inlinePrintNoQuotes(restored.toString());
    }
    catch(NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
  }
}
