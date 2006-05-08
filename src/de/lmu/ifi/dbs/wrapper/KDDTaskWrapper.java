package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;
import java.util.logging.Logger;

/**
 * KDDTaskWrapper is an abstract super class for all wrapper classes running
 * algorithms in a kdd task.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class KDDTaskWrapper implements Wrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handle options.
   */
  protected OptionHandler optionHandler;

  /**
   * Sets the flags for verbose and time and the parameter output in the
   * parameter map. Any extending class should call this constructor, add
   * further parameters and finally initialize optionHandler like this: <p/>
   * <p/>
   * <pre>
   *   {
   *       parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
   *       ...
   *       optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
   *   }
   * </pre>
   */
  protected KDDTaskWrapper() {
    parameterToDescription.put(KDDTask.OUTPUT_P
                               + OptionHandler.EXPECTS_VALUE, KDDTask.OUTPUT_D);
    parameterToDescription.put(AbstractAlgorithm.VERBOSE_F,
                               AbstractAlgorithm.VERBOSE_D);
    parameterToDescription.put(AbstractAlgorithm.TIME_F,
                               AbstractAlgorithm.TIME_D);

    optionHandler = new OptionHandler(parameterToDescription, this
    .getClass().getName());
  }

  /**
   * @see Wrapper#run(String[])
   */
  public final void run(String[] args) throws ParameterException {
    List<String> parameters = new ArrayList<String>(Arrays
    .asList(optionHandler.grabOptions(args)));

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(optionHandler.getOptionValue(KDDTask.OUTPUT_P));

    if (optionHandler.isSet(AbstractAlgorithm.TIME_F)) {
      parameters.add(OptionHandler.OPTION_PREFIX
                     + AbstractAlgorithm.TIME_F);
    }

    if (optionHandler.isSet(AbstractAlgorithm.VERBOSE_F)) {
      parameters.add(OptionHandler.OPTION_PREFIX
                     + AbstractAlgorithm.VERBOSE_F);
    }

    parameters.addAll(getParameters());
    KDDTask task = new KDDTask();
    task.setParameters(parameters.toArray(new String[parameters.size()]));
    task.run();
  }

  /**
   * Returns the parameters that are necessary to run this wrapper correctly.
   *
   * @return the array containing the parametr setting that is necessary to
   *         run this wrapper correctly
   */
  public abstract List<String> getParameters() throws ParameterException;
}
