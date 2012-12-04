package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * An OptionID is used by option handlers as a unique identifier for specific
 * options. There is no option possible without a specific OptionID defined
 * within this class.
 * 
 * @author Elke Achtert
 */
public final class OptionID {
  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -h}
   * </p>
   */
  public static final OptionID HELP = new OptionID("h", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -help}
   * </p>
   */
  public static final OptionID HELP_LONG = new OptionID("help", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.workflow.AlgorithmStep}
   */
  public static final OptionID ALGORITHM = new OptionID("algorithm", "Algorithm to run.");

  /**
   * Optional Parameter to specify a class to obtain a description for.
   * <p>
   * Key: {@code -description}
   * </p>
   */
  public static final OptionID DESCRIPTION = new OptionID("description", "Class to obtain a description of. " + "Causes immediate stop of the program.");

  /**
   * Optional Parameter to specify a class to enable debugging for.
   * <p>
   * Key: {@code -enableDebug}
   * </p>
   */
  public static final OptionID DEBUG = new OptionID("enableDebug", "Parameter to enable debugging for particular packages.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.workflow.InputStep}
   */
  public static final OptionID DATABASE = new OptionID("db", "Database class.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.workflow.InputStep}
   */
  // TODO: move to database class?
  public static final OptionID DATABASE_CONNECTION = new OptionID("dbc", "Database connection class.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.workflow.EvaluationStep}
   */
  public static final OptionID EVALUATOR = new OptionID("evaluator", "Class to evaluate the results with.");

  /**
   * OptionID for {@link de.lmu.ifi.dbs.elki.workflow.OutputStep}
   */
  public static final OptionID RESULT_HANDLER = new OptionID("resulthandler", "Result handler class.");

  /**
   * OptionID for the application output file/folder 
   */
  public static final OptionID OUTPUT = new OptionID("out", "Directory name (or name of an existing file) to write the obtained results in. " + "If this parameter is omitted, per default the output will sequentially be given to STDOUT.");

  /**
   * Flag to allow verbose messages while running the application.
   * <p>
   * Key: {@code -verbose}
   * </p>
   */
  public static final OptionID VERBOSE_FLAG = new OptionID("verbose", "Enable verbose messages.");

  /**
   * Flag to allow verbose messages while running the application.
   * <p>
   * Key: {@code -time}
   * </p>
   */
  public static final OptionID TIME_FLAG = new OptionID("time", "Enable logging of runtime data. Do not combine with more verbose logging, since verbose logging can significantly impact performance.");

  /**
   * Option name
   */
  private String name;

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
  public OptionID(final String name, final String description) {
    super();
    this.name = name;
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
    return new OptionID(name, description);
  }

  /**
   * Returns the name of this OptionID.
   * 
   * @return the name
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getName();
  }

  /**
   * Get the option name.
   * 
   * @return Option name
   */
  public String getName() {
    return name;
  }
}