package de.lmu.ifi.dbs.varianceanalysis;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Abstract super class for all eigenpair filters.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {

  /**
   * Default constructor for the abstract super class.
   */
  protected AbstractEigenPairFilter() {
	  super();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    return optionHandler.grabOptions(args);
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

}
