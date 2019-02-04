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
package de.lmu.ifi.dbs.elki.data.model;

/**
 * Model for an OPTICS cluster
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class OPTICSModel implements Model {
  /**
   * Start index
   */
  private int startIndex;

  /**
   * End index
   */
  private int endIndex;

  /**
   * @param startIndex
   * @param endIndex
   */
  public OPTICSModel(int startIndex, int endIndex) {
    super();
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  /**
   * Starting index of OPTICS cluster
   * 
   * @return index of cluster start
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * End index of OPTICS cluster
   * 
   * @return index of cluster end
   */
  public int getEndIndex() {
    return endIndex;
  }

  @Override
  public String toString() {
    return "OPTICSModel";
  }
}