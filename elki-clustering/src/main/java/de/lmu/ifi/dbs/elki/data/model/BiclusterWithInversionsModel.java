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

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * This code was factored out of the Bicluster class, since not all biclusters
 * have inverted rows.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class BiclusterWithInversionsModel extends BiclusterModel {
  /**
   * The ids of inverted rows.
   */
  private DBIDs invertedRows = null;

  /**
   * @param colIDs Col IDs
   */
  public BiclusterWithInversionsModel(int[] colIDs, DBIDs invertedRows) {
    super(colIDs);
    this.invertedRows = invertedRows;
  }

  /**
   * Sets the ids of the inverted rows.
   * 
   * @param invertedRows the ids of the inverted rows
   */
  public void setInvertedRows(DBIDs invertedRows) {
    this.invertedRows = DBIDUtil.makeUnmodifiable(invertedRows);
  }

  /**
   * Provides a copy of the inverted column IDs.
   * 
   * @return a copy of the inverted column IDs.
   */
  public DBIDs getInvertedRows() {
    return invertedRows;
  }
}
