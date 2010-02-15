package de.lmu.ifi.dbs.elki.normalization;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;

/**
 * Abstract super class for all normalizations.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public abstract class AbstractNormalization<O extends DatabaseObject> extends AbstractLoggable implements Normalization<O> {
  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    super();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
