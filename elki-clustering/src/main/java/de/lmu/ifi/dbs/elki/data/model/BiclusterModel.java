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
 * Wrapper class to provide the basic properties of a Bicluster.
 * 
 * @author Arthur Zimek
 * @since 0.6.0
 */
public class BiclusterModel implements Model {
  /**
   * The column numbers included in the Bicluster.
   */
  private int[] colIDs;

  /**
   * Defines a new Bicluster for given parameters.
   * 
   * @param colIDs the numbers of the columns included in the Bicluster
   */
  public BiclusterModel(int[] colIDs) {
    this.colIDs = colIDs;
  }

  /**
   * Provides a copy of the column IDs contributing to the Bicluster.
   * 
   * @return a copy of the columnsIDs
   */
  public int[] getColumnIDs() {
    return colIDs.clone();
  }
}
