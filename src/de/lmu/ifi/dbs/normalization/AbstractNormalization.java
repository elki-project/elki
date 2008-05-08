package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;

/**
 * Abstract super class for all normalizations.
 *
 * @author Elke Achtert
 */
public abstract class AbstractNormalization<O extends DatabaseObject> extends AbstractParameterizable implements Normalization<O> {

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
  public String toString() {
    return getClass().getName();
  }
}
