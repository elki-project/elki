package de.lmu.ifi.dbs.varianceanalysis.ica;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * A contrast function g in general is the derivative of a function G that is used to
 * approximate negentropy.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface ContrastFunction extends Parameterizable {

  /**
   * Computes the function value at position <code>x</code>.
   *
   * @param x the desired position
   * @return the function value
   */
  public double function(double x);

  /**
   * Computes the value of the function's derivative at
   * position <code>x</code>.
   *
   * @param x the desired position
   * @return the function value
   */
  public double derivative(double x);

}
