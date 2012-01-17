package de.lmu.ifi.dbs.elki.data.model;

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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * This code was factored out of the Bicluster class, since not all biclusters
 * have inverted rows. TODO: shouldn't that be inverted columns?
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class BiclusterWithInverted<V extends FeatureVector<V, ?>> extends Bicluster<V> {
  /**
   * The ids of inverted rows.
   */
  private ArrayModifiableDBIDs invertedRows;

  /**
   * @param rowIDs Row IDs
   * @param colIDs Col IDs
   * @param database Database
   */
  public BiclusterWithInverted(ArrayDBIDs rowIDs, int[] colIDs, Relation<V> database) {
    super(rowIDs, colIDs, database);
  }

  /**
   * @param rowIDs Row IDs
   * @param colIDs Col IDs
   * @param database Database
   * 
   * @deprecated Use DBIDs, not integer indexes!
   */
  @Deprecated
  public BiclusterWithInverted(int[] rowIDs, int[] colIDs, Relation<V> database) {
    super(rowIDs, colIDs, database);
  }

  /**
   * Sets the ids of the inverted rows.
   * 
   * @param invertedRows the ids of the inverted rows
   */
  public void setInvertedRows(DBIDs invertedRows) {
    this.invertedRows = DBIDUtil.newArray(invertedRows);
  }

  /**
   * Provides a copy of the inverted column IDs.
   * 
   * @return a copy of the inverted column IDs.
   */
  public DBIDs getInvertedRows() {
    return DBIDUtil.makeUnmodifiable(invertedRows);
  }

  /**
   * Sorts the row and column ids (and - if applicable - the ids of inverted
   * rows) in ascending order.
   */
  @Override
  public void sortIDs() {
    super.sortIDs();
    if(this.invertedRows != null) {
      this.invertedRows.sort();
    }
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    if(this.invertedRows != null) {
      out.commentPrintLn("inverted rows (row IDs): " + this.invertedRows.toString());
    }
  }
}