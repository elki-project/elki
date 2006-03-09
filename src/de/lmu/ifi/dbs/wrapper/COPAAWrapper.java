package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;

import java.util.List;
import java.util.ArrayList;

/**
 * Wrapper class for COPAA algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAAWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>the density threshold for clustering, " +
                                         "must be suitable to the distance function " +
                                         LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<minpts>a positive integer specifiying the minmum number of points in " +
                                        "one cluster";

  /**
   * Option string for parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * k.
   */
  protected int k;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public COPAAWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  public static void main(String[] args) {
    COPAAWrapper wrapper = new COPAAWrapper();
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

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) {
    String[] remainingParameters = super.setParameters(args);
    // epsilon
    epsilon = optionHandler.getOptionValue(COPAAWrapper.EPSILON_P);

    // minpts
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(COPAAWrapper.MINPTS_P));
      if (minpts <= 0) {
        WrongParameterValueException pfe = new WrongParameterValueException(COPAAWrapper.MINPTS_P, optionHandler.getOptionValue(COPAAWrapper.MINPTS_P));
        pfe.fillInStackTrace();
        throw pfe;
      }
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pfe = new WrongParameterValueException(COPAAWrapper.MINPTS_P, optionHandler.getOptionValue(COPAAWrapper.MINPTS_P));
      pfe.fillInStackTrace();
      throw pfe;
    }

    // k
    if (optionHandler.isSet(COPAAWrapper.K_P)) {
      try {
        k = Integer.parseInt(optionHandler.getOptionValue(COPAAWrapper.K_P));
      }
      catch (NumberFormatException e) {
        WrongParameterValueException pfe = new WrongParameterValueException(COPAAWrapper.K_P, optionHandler.getOptionValue(COPAAWrapper.K_P));
        pfe.fillInStackTrace();
        throw pfe;
      }
    }
    else {
      k = minpts;
    }

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    List<String> parameters = new ArrayList<String>(remainingParameters);

    // algorithm COPAA
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(COPAA.class.getName());

    // partition algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
//    params.add(DBSCAN.class.getName());
    parameters.add(OPTICS.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAC.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    parameters.add(Integer.toString(k));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }
}
