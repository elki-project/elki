package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Abstract super class for all eigenpair filters.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractEigenPairFilter implements EigenPairFilter {
  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler for handling options.
   */
  OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  String[] currentParameterArray = new String[0];

  /**
   * Default constructor for the abstract super class.
   */
  protected AbstractEigenPairFilter() {
    parameterToDescription = new Hashtable<String, String>();
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    return optionHandler.grabOptions(args);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    attributeSettings.add(mySettings);

    return attributeSettings;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.parameterDifference(complete, part);
  }

}
