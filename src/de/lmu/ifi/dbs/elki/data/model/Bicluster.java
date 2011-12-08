package de.lmu.ifi.dbs.elki.data.model;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Wrapper class to provide the basic properties of a bicluster.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector handled by this Result
 */
public class Bicluster<V extends FeatureVector<?, ?>> implements TextWriteable, Model {
  /**
   * The ids of the rows included in the bicluster.
   */
  private ArrayDBIDs rowIDs;

  /**
   * The ids of the rows included in the bicluster.
   */
  private int[] colIDs;

  /**
   * The database this bicluster is defined for.
   */
  private Relation<V> database;

  /**
   * Defines a new bicluster for given parameters.
   * 
   * @param rowIDs the ids of the rows included in the bicluster
   * @param colIDs the ids of the columns included in the bicluster
   * @param database the database this bicluster is defined for
   */
  public Bicluster(ArrayDBIDs rowIDs, int[] colIDs, Relation<V> database) {
    this.rowIDs = rowIDs;
    this.colIDs = colIDs;
    this.database = database;
  }

  /**
   * Defines a new bicluster for given parameters.
   * 
   * @param rowIDs the ids of the rows included in the bicluster
   * @param colIDs the ids of the columns included in the bicluster
   * @param database the database this bicluster is defined for
   * 
   * @deprecated Use DBIDs, not integers!
   */
  @Deprecated
  public Bicluster(int[] rowIDs, int[] colIDs, Relation<V> database) {
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(rowIDs.length);
    for(int rowid : rowIDs) {
      ids.add(DBIDUtil.importInteger(rowid));
    }
    this.rowIDs = ids;
    this.colIDs = colIDs;
    this.database = database;
  }

  /**
   * Sorts the row and column ids in ascending order.
   */
  public void sortIDs() {
    if(!(this.rowIDs instanceof ModifiableDBIDs)) {
      this.rowIDs = DBIDUtil.newArray(this.rowIDs);
    }
    Collections.sort((ArrayModifiableDBIDs) this.rowIDs);
    Arrays.sort(this.colIDs);
  }

  /**
   * The size of the cluster.
   * <p/>
   * The size of a bicluster is the number of included rows.
   * 
   * @return the size of the bicluster, i.e., the number or rows included in the
   *         bicluster
   */
  public int size() {
    return rowIDs.size();
  }

  /**
   * Provides an iterator for the row ids.
   * <p/>
   * Note that the iterator is not guaranteed to touch all elements if the
   * {@link #sortIDs()} is called during the lifetime of the iterator.
   * 
   * @return an iterator for the row ids
   */
  public Iterator<V> rowIterator() {
    return new Iterator<V>() {
      private int index = -1;

      @Override
      public boolean hasNext() {
        return index + 1 < size();
      }

      @Override
      @SuppressWarnings("synthetic-access")
      public V next() {
        return database.get(rowIDs.get(++index));
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_REMOVE);
      }

    };
  }

  /**
   * Creates a DBIDs for the row IDs included in this Bicluster.
   * 
   * 
   * @return a DBIDs for the row IDs included in this Bicluster
   */
  public DBIDs getDatabaseObjectGroup() {
    return this.rowIDs;
  }

  /**
   * Getter to retrieve the database
   * 
   * @return Database
   */
  public Relation<V> getDatabase() {
    return database;
  }

  /**
   * Provides a copy of the column IDs contributing to the bicluster.
   * 
   * @return a copy of the columnsIDs
   */
  public int[] getColumnIDs() {
    int[] columnIDs = new int[colIDs.length];
    System.arraycopy(colIDs, 0, columnIDs, 0, colIDs.length);
    return columnIDs;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Serialization class: " + this.getClass().getName());
    out.commentPrintLn("Cluster size: " + size());
    out.commentPrintLn("Cluster dimensions: " + colIDs.length);
    out.commentPrintLn("Included row IDs: " + rowIDs.toString());
    out.commentPrintLn("Included column IDs: " + FormatUtil.format(colIDs));
  }
}
