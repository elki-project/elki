/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * An OptionID is used by option handlers as a unique identifier for specific
 * options. There is no option possible without a specific OptionID defined
 * within this class.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public final class OptionID {
  /**
   * Option name
   */
  private String name;

  /**
   * The description of the OptionID.
   */
  private String description;

  /**
   * Constructor.
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