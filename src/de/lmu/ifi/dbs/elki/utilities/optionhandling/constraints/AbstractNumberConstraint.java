package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/**
 * Abstract super class for constraints dealing with a certain number value.
 *
 * @author Elke Achtert
 * @param <P> the type of the parameter to be tested by this constraint (e.g., Number, List<Number>)
 */
public abstract class AbstractNumberConstraint<P> implements ParameterConstraint<P> {

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
    this.constraintValue = constraintValue;
  }
}
