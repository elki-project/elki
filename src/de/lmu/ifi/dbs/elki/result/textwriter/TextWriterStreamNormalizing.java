package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.HandlerList;

/**
 * Normalizing version of {@link TextWriterStream}
 * @author Erich Schubert
 *
 * @param <O> Object type that can be normalized.
 */
// TODO: Allow multiple normalization functions for different objects AND/OR databases. 
public class TextWriterStreamNormalizing<O extends DatabaseObject> extends TextWriterStream {
  /**
   * Normalization function
   */
  private Normalization<O> normalization;

  /**
   * Constructor.
   * 
   * @param out Output stream
   * @param writers Object writers
   * @param normalization Normalization
   */
  public TextWriterStreamNormalizing(PrintStream out, HandlerList<TextWriterWriterInterface<?>> writers, Normalization<O> normalization) {
    super(out, writers);
    this.normalization = normalization;
  }
  
  /**
   * Normalize output.
   * 
   * @param v
   * @return
   * @throws NonNumericFeaturesException
   */
  public O normalizationRestore(O v) throws NonNumericFeaturesException {
    if (getNormalization() == null) return v;
    return getNormalization().restore(v);
  }

  /**
   * Setter for normalization.
   * 
   * @param normalization
   */
  public void setNormalization(Normalization<O> normalization) {
    this.normalization = normalization;
  }

  /**
   * Getter for normalization class.
   * @return
   */
  public Normalization<O> getNormalization() {
    return normalization;
  }
}
