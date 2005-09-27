package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.ORCLUS;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;

/**
 * Wrapper class for ORCLUS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ORCLUSWrapper extends AbstractWrapper {
  /**
   * Remaining parameters.
   */
  private String[] remainingParams;

  /**
   * The dim parameter.
   */
  private String dim;

  /**
   * The k parameter.
   */
  private String k;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public ORCLUSWrapper() {
    super();
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
    return remainingParams;
  }

  /**
   * Runs the ORCLUS algorithm.
   */
  public void runORCLUS() {
    if (output == null)
      throw new IllegalArgumentException("Parameter " + AbstractWrapper.OUTPUT_P + " is not set!");

    ArrayList<String> params = new ArrayList<String>();
    for (String s : remainingParams) {
      params.add(s);
    }

    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(ORCLUS.class.getName());

    params.add(OptionHandler.OPTION_PREFIX + ORCLUS.DIM_P);
    params.add(dim);

    params.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
    params.add(k);

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
//      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
//      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
//      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
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
    String inputDir = this.input;
    String outputDir = this.output;

    for (int i = 5; i <= 50; i += 5) {
      for (int d = 1; d < i; d++) {
        this.input = inputDir + "/dim" + i;
        this.output = outputDir + "/ORCLUSdim" + i + "_" + d;


        this.dim = Integer.toString(d);
        this.k = Integer.toString(i);

        System.out.println("dimensionality " + i + "_"  + d);
        this.runORCLUS();
      }
    }

    // for (int i = 5; i <= 50; i += 5) {
    // this.input = inputDir + "/size" + i;
    // this.output = outputDir + "/size" + i;
    // this.minpts = Integer.toString(3 * i);
    // System.out.println("size " + i);
    // this.runCOPAC();
    // }

  }

  public static void main(String[] args) {
    ORCLUSWrapper copac = new ORCLUSWrapper();
    try {
      copac.run(args);
    }
    catch (AbortException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }
    catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    catch (IllegalStateException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
