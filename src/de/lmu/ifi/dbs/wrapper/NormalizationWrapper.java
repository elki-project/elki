package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;

/**
 * NormalizationWrapper is an abstract super class for all
 * file based database connection wrappers that need to normalize the input data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class NormalizationWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
