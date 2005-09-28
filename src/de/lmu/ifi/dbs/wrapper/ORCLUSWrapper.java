package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.*;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class for COPAC algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends AbstractWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the distance function " + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * Remaining parameters.
   */
  private String[] remainingParams;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public ORCLUSWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    remainingParams = super.setParameters(args);
    try {
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = optionHandler.getOptionValue(MINPTS_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }

  /**
   * Runs the COPAC algorithm.
   */
  public void runCOPAC() {
    if (output == null) {
      throw new IllegalArgumentException("Parameter " + AbstractWrapper.OUTPUT_P + " is not set!");
    }
    ArrayList<String> params = new ArrayList<String>();
    for (String s : remainingParams) {
      params.add(s);
    }

    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(COPAC.class.getName());

    params.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    params.add(DBSCAN.class.getName());

    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    params.add(LocallyWeightedDistanceFunction.class.getName());

    params.add(OptionHandler.OPTION_PREFIX + COPAC.PREPROCESSOR_P);
    params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    params.add(epsilon);

    params.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    params.add(minpts);

//    params.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
//    params.add(minpts);

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseDoubleVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

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

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }


  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {

    this.setParameters(args);
    this.runCOPAC();
  }

  public static void main(String[] args) {
    ORCLUSWrapper copac = new ORCLUSWrapper();
    try {
      copac.run(args);
    }
    catch (AbortException e) {
      System.out.println(e.getMessage());
    }
    catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }
    catch (IllegalStateException e) {
      System.err.println(e.getMessage());
    }
  }
}
