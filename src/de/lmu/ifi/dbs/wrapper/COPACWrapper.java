package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterFormatException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * Wrapper class for COPAC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPACWrapper extends AbstractWrapper {
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
  public COPACWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  public static void main(String[] args) {
    COPACWrapper copac = new COPACWrapper();
    try {
      copac.run(args);
    }
    catch (ParameterFormatException e) {
      System.err.println(copac.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(copac.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(copac.optionHandler.usage(e.getMessage()));
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
    super.setParameters(args);
    // epsilon
    epsilon = optionHandler.getOptionValue(EPSILON_P);

    // minpts
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
      if (minpts <= 0) {
        ParameterFormatException pfe = new ParameterFormatException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
        pfe.fillInStackTrace();
        throw pfe;
      }
    }
    catch (NumberFormatException e) {
      ParameterFormatException pfe = new ParameterFormatException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
      pfe.fillInStackTrace();
      throw pfe;
    }

    // k
    if (optionHandler.isSet(K_P)) {
      try {
        k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      }
      catch (NumberFormatException e) {
        ParameterFormatException pfe = new ParameterFormatException(K_P, optionHandler.getOptionValue(K_P));
        pfe.fillInStackTrace();
        throw pfe;
      }
    }
    else {
      k = minpts;
    }

    return new String[0];
  }

  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);

    List<String> params = initParameters();
    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Initializes the parameters.
   *
   * @return an array containing the parameters to run the algorithm.
   */
  protected List<String> initParameters() {
    List<String> params = getRemainingParameters();

    // algorithm COPAC
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(COPAC.class.getName());

    // partition algorithm DBSCAN
    params.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
//    params.add(DBSCAN.class.getName());
    params.add(OPTICS.class.getName());

    // epsilon
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    params.add(epsilon);

    // minpts
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    params.add(Integer.toString(minpts));

    // distance function
    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    params.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    params.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    params.add(OptionHandler.OPTION_PREFIX + COPAC.PREPROCESSOR_P);
    params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k
    params.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    params.add(Integer.toString(k));

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseRealVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return params;
  }
}
