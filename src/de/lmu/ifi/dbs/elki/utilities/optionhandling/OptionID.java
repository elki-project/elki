package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.ConstantObject;

/**
 * An OptionID is used by option handlers as a unique identifier for specific
 * options. There is no option possible without a specific OptionID defined
 * within this class.
 * 
 * @author Elke Achtert
 */
public final class OptionID extends ConstantObject<OptionID> {
  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication#HELP_FLAG}
   */
  public static final OptionID HELP = new OptionID("h", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication#HELP_LONG_FLAG}
   */
  public static final OptionID HELP_LONG = new OptionID("help", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.KDDTask#ALGORITHM_PARAM}
   */
  public static final OptionID ALGORITHM = new OptionID("algorithm", "Algorithm to run.");

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication#DESCRIPTION_PARAM}
   */
  public static final OptionID DESCRIPTION = new OptionID("description", "Class to obtain a description of. " + "Causes immediate stop of the program.");

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication#DEBUG_PARAM}
   */
  public static final OptionID DEBUG = new OptionID("enableDebug", "Parameter to enable debugging for particular packages.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.KDDTask#DATABASE_CONNECTION_PARAM}
   */
  public static final OptionID DATABASE_CONNECTION = new OptionID("dbc", "Database connection class.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.KDDTask#RESULT_HANDLER_PARAM}
   */
  public static final OptionID RESULT_HANDLER = new OptionID("resulthandler", "Result handler class.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.result.ResultWriter#OUTPUT_PARAM}
   */
  public static final OptionID OUTPUT = new OptionID("out", "Directory name (or name of an existing file) to write the obtained results in. " + "If this parameter is omitted, per default the output will sequentially be given to STDOUT.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.KDDTask#NORMALIZATION_PARAM}
   */
  public static final OptionID NORMALIZATION = new OptionID("norm", "Normalization class in order to normalize values in the database.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.KDDTask#NORMALIZATION_PARAM}
   */
  public static final OptionID NORMALIZATION_UNDO = new OptionID("normUndo", "Revert normalization result to original values - " + "invalid option if no normalization has been performed.");

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#VERBOSE_FLAG}
   */
  public static final OptionID ALGORITHM_VERBOSE = new OptionID("verbose", "Enable verbose messages while performing the algorithm.");

  /**
   * OptionID for
   * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#TIME_FLAG}
   */
  public static final OptionID ALGORITHM_TIME = new OptionID("time", "Request output of performance time.");

  /**
   * The description of the OptionID.
   */
  private String description;

  /**
   * Provides a new OptionID of the given name and description.
   * <p/>
   * All OptionIDs are unique w.r.t. their name. An OptionID provides
   * additionally a description of the option.
   * 
   * @param name the name of the option
   * @param description the description of the option
   */
  private OptionID(final String name, final String description) {
    super(name);
    this.description = description;
  }

  /**
   * Returns the description of this OptionID.
   * 
   * @return the description of this OptionID
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of this OptionID.
   * 
   * @param description the description to be set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets or creates the OptionID for the given class and given name. The
   * OptionID usually is named as the classes name (lowercase) as name-prefix
   * and the given name as suffix of the complete name, separated by a dot. For
   * example, the parameter {@code epsilon} for the class
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN} will be named
   * {@code dbscan.epsilon}.
   * 
   * @param name the name
   * @param description the description is also set if the named OptionID does
   *        exist already
   * @return the OptionID for the given name
   */
  public static OptionID getOrCreateOptionID(final String name, final String description) {
    OptionID optionID = getOptionID(name);
    if(optionID == null) {
      optionID = new OptionID(name, description);
    }
    else {
      optionID.setDescription(description);
    }
    return optionID;
  }

  /**
   * Returns the OptionID for the given name if it exists, null otherwise.
   * 
   * @param name name of the desired OptionID
   * @return the OptionID for the given name
   */
  public static OptionID getOptionID(final String name) {
    return OptionID.lookup(OptionID.class, name);
  }
}