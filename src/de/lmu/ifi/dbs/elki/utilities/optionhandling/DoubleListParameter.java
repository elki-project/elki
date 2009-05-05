package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Parameter class for a parameter specifying a list of double values.
 * 
 * @author Steffi Wanka
 */
public class DoubleListParameter extends ListParameter<Double> {
  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   */
  public DoubleListParameter(OptionID optionID) {
    super(optionID);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID
   * @param constraint
   * @param optional
   * @param defaultValue
   */
  public DoubleListParameter(OptionID optionID, ParameterConstraint<List<Double>> constraint, boolean optional, List<Double> defaultValue) {
    super(optionID, constraint, optional, defaultValue);
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

  @Override
  public void setValue(String value) throws ParameterException {
    if(isValid(value)) {
      String[] values = SPLIT.split(value);
      Vector<Double> doubleValue = new Vector<Double>();
      for(String val : values) {
        doubleValue.add(Double.parseDouble(val));
      }
      this.value = doubleValue;
    }
  }

  @Override
  public boolean isValid(String value) throws ParameterException {
    String[] values = SPLIT.split(value);
    if(values.length == 0) {

      throw new UnspecifiedParameterException("Wrong parameter format! Given list of double values for parameter \"" + getName() + "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
    }

    // list for checking the parameter constraints
    List<Double> doubleList = new ArrayList<Double>();
    for(String val : values) {
      try {
        Double.parseDouble(val);
        doubleList.add(Double.parseDouble(val));
      }
      catch(NumberFormatException e) {
        throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given parameter " + val + " is no double!\n");
      }
    }

    for(ParameterConstraint<List<Double>> cons : this.constraints) {
      cons.test(doubleList);
    }

    return true;
  }

  /**
   * Sets the default value of this parameter.
   * 
   * @param allListDefaultValue default value for all list elements of this
   *        parameter
   */
  public void setDefaultValue(double allListDefaultValue) {
    for(int i = 0; i < defaultValue.size(); i++) {
      defaultValue.set(i, allListDefaultValue);
    }
  }

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
