package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Write a pair
 * 
 * @author Erich Schubert
 *
 */
public class TextWriterPair extends TextWriterWriterInterface<Pair<?,?>> {
  /**
   * Serialize a pair, component-wise
   */
  @Override
  @SuppressWarnings("unchecked")
  public void write(TextWriterStream out, String label, Pair<?,?> object) throws UnableToComplyException, IOException {
    if (object != null) {
      Object first = object.getFirst();
      if (first != null) {
        TextWriterWriterInterface<Object> tw = (TextWriterWriterInterface<Object>) out.getWriterFor(first);
        if (tw == null) {
          throw new UnableToComplyException("No handler for database object itself: " + first.getClass().getSimpleName());
        }
        tw.write(out, label, first);
      }
      Object second = object.getSecond();
      if (second != null) {
        TextWriterWriterInterface<Object> tw = (TextWriterWriterInterface<Object>) out.getWriterFor(second);
        if (tw == null) {
          throw new UnableToComplyException("No handler for database object itself: " + second.getClass().getSimpleName());
        }
        tw.write(out, label, second);
      }
    }
  }
}
