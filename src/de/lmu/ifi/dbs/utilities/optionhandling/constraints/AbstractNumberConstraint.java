package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

/**
 * Abstract super class for constraints dealing with a certain number value.
 *
 * @author Elke Achtert
 * @param <P> the type of the parameter to be tested by this constraint (e.g., Number, List<Number>)
 */
public abstract class AbstractNumberConstraint<P> extends AbstractLoggable implements ParameterConstraint<P> {

  /**
   * The constraint value.
   */
  final Number constraintValue;

  /**
   * Creates an abstract number constraint.
   *
   * @param constraintValue the constraint value
   */
  public AbstractNumberConstraint(Number constraintValue) {
    super(LoggingConfiguration.DEBUG);
    this.constraintValue = constraintValue;
  }
}
