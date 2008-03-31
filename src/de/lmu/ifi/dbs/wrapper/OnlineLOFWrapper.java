package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.outlier.OnlineLOF;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.io.File;
import java.util.List;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert 
 */
public class OnlineLOFWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * The value of the minpts parameter.
   */
  private int minpts;

  /**
   * The value of the insertions parameter.
   */
  private File insertions;

  /**
   * The value of the lof parameter.
   */
  private File lof;

  /**
   * The value of the nn parameter.
   */
  private File nn;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    OnlineLOFWrapper wrapper = new OnlineLOFWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      cause.printStackTrace();
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameters epsilon and minpts in the parameter map additionally
   * to the parameters provided by super-classes.
   */
  public OnlineLOFWrapper() {
    super();
    // parameter min points
    optionHandler.put(new IntParameter(OnlineLOF.MINPTS_P, OnlineLOF.MINPTS_D, new GreaterConstraint(0)));

    // parameter insertions
    optionHandler.put(new FileParameter(OnlineLOF.INSERTIONS_P, OnlineLOF.INSERTIONS_D, FileParameter.FILE_IN));

    // parameter LOF
    optionHandler.put(new FileParameter(OnlineLOF.LOF_P, OnlineLOF.LOF_D, FileParameter.FILE_IN));

    //parameter nn
    optionHandler.put(new FileParameter(OnlineLOF.NN_P, OnlineLOF.NN_D, FileParameter.FILE_IN));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm OnlineLOF
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OnlineLOF.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // insertions
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.INSERTIONS_P);
    parameters.add(insertions.getPath());

    // lof
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.LOF_P);
    parameters.add(lof.getPath());

    // nn
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.NN_P);
    parameters.add(nn.getPath());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.DISTANCE_FUNCTION_P);
    parameters.add(EuklideanDistanceFunction.class.getName());

    // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.PAGE_SIZE_P);
//    parameters.add("8000");

    // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);


    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    minpts = (Integer) optionHandler.getOptionValue(OnlineLOF.MINPTS_P);
    insertions = (File) optionHandler.getOptionValue(OnlineLOF.INSERTIONS_P);
    lof = (File) optionHandler.getOptionValue(OnlineLOF.LOF_P);
    nn = (File) optionHandler.getOptionValue(OnlineLOF.NN_P);

    return remainingParameters;
  }
}
