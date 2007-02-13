package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.IndexDatabase;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * AbstractAlgorithm sets the values for flags verbose and time. <p/> Any
 * Algorithm that makes use of these flags may extend this class. Beware to make
 * correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractAlgorithm<O extends DatabaseObject> extends
    AbstractParameterizable implements Algorithm<O> {

  /**
   * Flag to allow verbose messages.
   */
  public static final String VERBOSE_F = "verbose";

  /**
   * Description for verbose flag.
   */
  public static final String VERBOSE_D = "flag to allow verbose messages while performing the algorithm";

  /**
   * Flag to assess runtime.
   */
  public static final String TIME_F = "time";

  /**
   * Description for time flag.
   */
  public static final String TIME_D = "flag to request output of performance time";

  /**
   * Property whether verbose messages should be allowed.
   */
  private boolean verbose;

  /**
   * Property whether runtime should be assessed.
   */
  private boolean time;

  /**
   * Sets the flags for verbose and time in the parameter map. Any extending
   * class should call this constructor, then add further parameters.
   * Subclasses can add further parameters using one of the put-methods of the OptionHandler:
   * <ul>
   * <li>{@link de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler#put(java.util.Map)}</li>
   * <li>{@link de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler#put(de.lmu.ifi.dbs.utilities.optionhandling.Option)}</li>
   * <li>{@link de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler#put(String, de.lmu.ifi.dbs.utilities.optionhandling.Option)}</li>
   * </ul>
   */
  protected AbstractAlgorithm() {
    super();

    optionHandler.put(VERBOSE_F, new Flag(VERBOSE_F, VERBOSE_D));
    optionHandler.put(TIME_F, new Flag(TIME_F, TIME_D));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return optionHandler.usage("", false);
  }

  /**
   * Sets the values for verbose and time flags. Any extending class should
   * call this method first and return the returned array without further
   * changes, but after setting further required parameters. An example
   * for overwritting this method taking advantage from the previously (in
   * superclasses) defined options would be:
   * <pre>
   *            {
   *                String[] remainingParameters = super.setParameters(args);
   *                // set parameters for your class eventually using optionHandler
   *                .
   *                .
   *                .
   *                return remainingParameters;
   *                // or in case of attributes requesting parameters themselves
   *                // return parameterizableAttribbute.setParameters(remainingParameters);
   *            }
   * </pre>
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    verbose = optionHandler.isSet(VERBOSE_F);
    time = optionHandler.isSet(TIME_F);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();

    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(TIME_F, Boolean.toString(isTime()));
    mySettings.addSetting(VERBOSE_F, Boolean.toString(isVerbose()));

    return settings;
  }

  /**
   * Returns whether the time should be assessed.
   *
   * @return whether the time should be assessed
   */
  public boolean isTime() {
    return time;
  }

  /**
   * Returns whether verbose messages should be printed while executing the
   * algorithm.
   *
   * @return whether verbose messages should be printed while executing the
   *         algorithm
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets whether the time should be assessed.
   *
   * @param time whether the time should be assessed
   */
  public void setTime(boolean time) {
    this.time = time;
  }

  /**
   * Sets whether verbose messages should be printed while executing the
   * algorithm.
   *
   * @param verbose whether verbose messages should be printed while executing the
   *                algorithm
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Calls the runInTime()-method of extending classes. Measures and prints
   * the runtime of this method.
   *
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  public final void run(Database<O> database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    runInTime(database);
    long end = System.currentTimeMillis();
    if (isTime()) {
      long elapsedTime = end - start;
      verbose(this.getClass().getName() + " runtime  : " + elapsedTime
              + " milliseconds.");

    }
    if (database instanceof IndexDatabase && isTime()) {
      IndexDatabase db = (IndexDatabase) database;
      StringBuffer msg = new StringBuffer();
      msg.append(getClass().getName() + " physical read access : "
                 + db.getPhysicalReadAccess() + "\n");
      msg.append(getClass().getName() + " physical write access : "
                 + db.getPhysicalWriteReadAccess() + "\n");
      msg.append(getClass().getName() + " logical page access : "
                 + db.getLogicalPageAccess() + "\n");
      verbose(msg.toString());
    }
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database
   *            the database to run the algorithm on
   * @throws IllegalStateException
   *             if the algorithm has not been initialized properly (e.g. the
   *             setParameters(String[]) method has been failed to be called).
   */
  protected abstract void runInTime(Database<O> database)
      throws IllegalStateException;

}
