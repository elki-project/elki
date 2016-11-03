package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a list of double values.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class DoubleListParameter extends ListParameter<DoubleListParameter, double[]> {
  /**
   * Constructs a list parameter with the given optionID and optional flag.
   * 
   * @param optionID Option ID
   * @param optional Optional flag
   */
  public DoubleListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID Option ID
   */
  public DoubleListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return FormatUtil.format(getValue(), LIST_SEP);
  }

  @Override
  public String getDefaultValueAsString() {
    return FormatUtil.format(getDefaultValue(), LIST_SEP);
  }

  @Override
  protected double[] parseValue(Object obj) throws ParameterException {
    if(obj instanceof double[]) {
      return double[].class.cast(obj);
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      double[] doubleValue = new double[values.length];
      for(int i = 0; i < values.length; i++) {
        doubleValue[i] = ParseUtil.parseDouble(values[i]);
      }
      return doubleValue;
    }
    if(obj instanceof Double) {
      return new double[] { (Double) obj };
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Double values!");
  }

  @Override
  public int size() {
    return getValue().length;
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
