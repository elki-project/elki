package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.ORCLUS;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for COPAC algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<integer> value to specify the number of clusters to be found";

  /**
   * Parameter l.
   */
  public static final String DIM_P = "dim";

  /**
   * Description for parameter l.
   */
  public static final String DIM_D = "<integer> value to specify the dimensionality of the clusters to be found";


  /**
   * Parameter k.
   */
  private int k;

  /**
   * Parameter dim.
   */
  private int dim;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public ORCLUSWrapper() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    parameterToDescription.put(DIM_P + OptionHandler.EXPECTS_VALUE, DIM_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Sets the parameters k and dim additionally to the parameters set
   * by the super-class' method. Both k and dim are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    try {
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pe = new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D);
      pe.fillInStackTrace();
      throw pe;
    }

    try {
      dim = Integer.parseInt(optionHandler.getOptionValue(DIM_P));
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pe = new WrongParameterValueException(DIM_P, optionHandler.getOptionValue(DIM_P), DIM_D);
      pe.fillInStackTrace();
      throw pe;
    }

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    List<String> parameters = new ArrayList<String>(remainingParameters);

    // ORCLUS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(ORCLUS.class.getName());

    // dim
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.DIM_P);
    parameters.add(Integer.toString(dim));

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
    parameters.add(Integer.toString(k));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // db-connection
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // out
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }

  public static void main(String[] args) {
    ORCLUSWrapper wrapper = new ORCLUSWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }
}
