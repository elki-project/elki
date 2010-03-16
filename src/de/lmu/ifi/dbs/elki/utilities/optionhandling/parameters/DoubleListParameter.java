package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Parameter class for a parameter specifying a list of double values.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class DoubleListParameter extends ListParameter<Double> {
  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraints
   * @param defaultValue
   */
  public DoubleListParameter(OptionID optionID, List<ParameterConstraint<List<Double>>> constraints, List<Double> defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraints
   * @param optional
   */
  public DoubleListParameter(OptionID optionID, List<ParameterConstraint<List<Double>>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraints
   */
  /*
   * public DoubleListParameter(OptionID optionID,
   * List<ParameterConstraint<List<Double>>> constraints) { super(optionID,
   * constraints); }
   */

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraint
   * @param defaultValue
   */
  public DoubleListParameter(OptionID optionID, ParameterConstraint<List<Double>> constraint, List<Double> defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraint
   * @param optional
   */
  public DoubleListParameter(OptionID optionID, ParameterConstraint<List<Double>> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraint
   */
  public DoubleListParameter(OptionID optionID, ParameterConstraint<List<Double>> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a list parameter with the given optionID and optional flag.
   * 
   * @param optionID
   * @param optional
   */
  public DoubleListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   */
  public DoubleListParameter(OptionID optionID) {
    super(optionID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return FormatUtil.format(getValue().toArray(new Double[0]));
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected List<Double> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for (Object o : l) {
        if (!(o instanceof Double)) {
          throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given list contains objects of different type!");
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list?
      return (List<Double>)l;
    } catch (ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      ArrayList<Double> doubleValue = new ArrayList<Double>(values.length);
      for(String val : values) {
        doubleValue.add(Double.parseDouble(val));
      }
      return doubleValue;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Double values!");
  }

  /**
   * Sets the default value of this parameter.
   * 
   * @param allListDefaultValue default value for all list elements of this
   *        parameter
   */
  // Unused?
  /*public void setDefaultValue(double allListDefaultValue) {
    for(int i = 0; i < defaultValue.size(); i++) {
      defaultValue.set(i, allListDefaultValue);
    }
  }*/

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;double_1,...,double_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<double_1,...,double_n>";
  }
}
