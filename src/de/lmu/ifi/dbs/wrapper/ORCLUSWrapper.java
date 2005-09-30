package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.ORCLUS;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
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
    super.setParameters(args);
    try {
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      dim = Integer.parseInt(optionHandler.getOptionValue(DIM_P));
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
   * Runs the ORCLUS algorithm.
   */
  public void runORCLUS() {
    ArrayList<String> params = getRemainingParameters();

    // ORCLUS algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(ORCLUS.class.getName());

    // dim
    params.add(OptionHandler.OPTION_PREFIX + ORCLUS.DIM_P);
    params.add(Integer.toString(dim));

    // k
    params.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
    params.add(Integer.toString(k));

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseDoubleVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // db
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // out
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }


  /**
   * Runs the ORCLUS algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);
    this.runORCLUS();
  }

  public static void main(String[] args) {
    ORCLUSWrapper orclus = new ORCLUSWrapper();
    try {
      orclus.run(args);
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
