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
package de.lmu.ifi.dbs.elki.result;

/**
 * Basic class for a result. Much like AbstractHierarchicalResult, except it
 * stores the required short and long result names.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 */
// TODO: getter, setter for result names? Merge with AbstractHierarchicalResult?
public class BasicResult extends AbstractHierarchicalResult {
  /**
   * Result name, for presentation
   */
  private String name;

  /**
   * Result name, for output
   */
  private String shortname;

  /**
   * Result constructor.
   *
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public BasicResult(String name, String shortname) {
    super();
    this.name = name;
    this.shortname = shortname;
  }

  @Override
  public final String getLongName() {
    return name;
  }

  @Override
  public final String getShortName() {
    return shortname;
  }
}