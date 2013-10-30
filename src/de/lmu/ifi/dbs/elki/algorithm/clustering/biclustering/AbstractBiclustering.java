package de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Bicluster;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Abstract class as a convenience for different biclustering approaches.
 * <p/>
 * The typically required values describing submatrices are computed using the
 * corresponding values within a database of NumberVectors.
 * <p/>
 * The database is supposed to present a data matrix with a row representing an
 * entry ({@link FeatureVector}), a column representing a dimension (attribute)
 * of the {@link FeatureVector}s.
 * 
 * @author Arthur Zimek
 * @param <V> a certain subtype of NumberVector - the data matrix is supposed to
 *        consist of rows where each row relates to an object of type V and the
 *        columns relate to the attribute values of these objects
 * @param <M> Cluster model type
 */
public abstract class AbstractBiclustering<V extends NumberVector<?>, M extends Bicluster<V>> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>> {
  /**
   * Keeps the currently set database.
   */
  private Database database;

  /**
   * Relation we use.
   */
  protected Relation<V> relation;
  
  /**
   * Iterator to use for more efficient random access.
   */
  private DBIDArrayIter iter;

  /**
   * The row ids corresponding to the currently set {@link #relation}.
   */
  private ArrayDBIDs rowIDs;

  /**
   * Column dimensionality.
   */
  private int colDim;

  /**
   * Constructor.
   */
  protected AbstractBiclustering() {
    super();
  }

  /**
   * Prepares the algorithm for running on a specific database.
   * <p/>
   * Assigns the database, the row ids, and the col ids, then calls
   * {@link #biclustering()}.
   * <p/>
   * Any concrete algorithm should be implemented within method
   * {@link #biclustering()} by an inheriting biclustering approach.
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  public final Clustering<M> run(Relation<V> relation) {
    this.relation = relation;
    if(this.relation == null || this.relation.size() == 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    colDim = RelationUtil.dimensionality(relation);
    rowIDs = DBIDUtil.ensureArray(this.relation.getDBIDs());
    iter = rowIDs.iter();
    return biclustering();
  }

  /**
   * Any concrete biclustering algorithm should be implemented within this
   * method. The database of double-valued <code>NumberVector</code>s is
   * encapsulated, methods {@link #sortRows(int,int,List,Comparator)},
   * {@link #sortCols(int,int,List,Comparator)},
   * {@link #meanOfBicluster(BitSet,BitSet)}, {@link #meanOfRow(int,BitSet)},
   * {@link #meanOfCol(BitSet,int)}, {@link #valueAt(int,int)}, allow typical
   * operations like on a data matrix.
   * <p/>
   * This method is supposed to be called only from the method
   * {@link #run(Database)}.
   * <p/>
   * If a bicluster is to be appended to the result, the methods
   * {@link #defineBicluster(BitSet,BitSet)} and
   * {@link #addBiclusterToResult(Bicluster)} should be used.
   */
  protected abstract Clustering<M> biclustering();

  /**
   * Convert a bitset into integer column ids.
   * 
   * @param cols
   * @return integer column ids
   */
  protected int[] colsBitsetToIDs(BitSet cols) {
    int[] colIDs = new int[cols.cardinality()];
    int colsIndex = 0;
    for(int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
      colIDs[colsIndex] = i;
      colsIndex++;
    }
    return colIDs;
  }

  /**
   * Convert a bitset into integer row ids.
   * 
   * @param rows
   * @return integer row ids
   */
  protected ArrayDBIDs rowsBitsetToIDs(BitSet rows) {
    ArrayModifiableDBIDs rowIDs = DBIDUtil.newArray(rows.cardinality());
    DBIDArrayIter iter = this.rowIDs.iter();
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      iter.seek(i);
      rowIDs.add(iter);
    }
    return rowIDs;
  }

  /**
   * Defines a bicluster as given by the included rows and columns.
   * 
   * @param rows the rows included in the bicluster
   * @param cols the columns included in the bicluster
   * @return a bicluster as given by the included rows and columns
   */
  protected Bicluster<V> defineBicluster(BitSet rows, BitSet cols) {
    ArrayDBIDs rowIDs = rowsBitsetToIDs(rows);
    int[] colIDs = colsBitsetToIDs(cols);
    return new Bicluster<>(rowIDs, colIDs, relation);
  }

  /**
   * Adds the given Bicluster to the result of this Biclustering.
   * 
   * @param result Result to add to
   * @param bicluster the bicluster to add to the result
   */
  protected void addBiclusterToResult(Clustering<M> result, M bicluster) {
    result.addToplevelCluster(new Cluster<>(bicluster.getDatabaseObjectGroup(), bicluster));
  }

  /**
   * Returns the value of the data matrix at row <code>row</code> and column
   * <code>col</code>.
   * 
   * @param row the row in the data matrix according to the current order of
   *        rows (refers to database entry
   *        <code>database.get(rowIDs[row])</code>)
   * @param col the column in the data matrix according to the current order of
   *        rows (refers to the attribute value of an database entry
   *        <code>getValue(colIDs[col])</code>)
   * @return the attribute value of the database entry as retrieved by
   *         <code>database.get(rowIDs[row]).getValue(colIDs[col])</code>
   */
  protected double valueAt(int row, int col) {
    iter.seek(row);
    return relation.get(iter).doubleValue(col);
  }
  
  /**
   * Get the DBID of a certain row
   * 
   * @param row Row number
   * @return DBID of this row
   * @deprecated Expensive!
   */
  @Deprecated
  protected DBID getRowDBID(int row) {
    return rowIDs.get(row);
  }

  /**
   * Provides the mean value for a row on a set of columns. The columns are
   * specified by a BitSet where the indices of a set bit relate to the indices
   * in {@link #colIDs}.
   * 
   * @param row the row to compute the mean value w.r.t. the given set of
   *        columns (relates to database entry id
   *        <code>{@link #rowIDs rowIDs[row]}</code>)
   * @param cols the set of columns to include in the computation of the mean of
   *        the given row
   * @return the mean value of the specified row over the specified columns
   */
  protected double meanOfRow(int row, BitSet cols) {
    Mean m = new Mean();
    for(int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
      m.put(valueAt(row, i));
    }
    return m.getMean();
  }

  /**
   * Provides the mean value for a column on a set of rows. The rows are
   * specified by a BitSet where the indices of a set bit relate to the indices
   * in {@link #rowIDs}.
   * 
   * @param rows the set of rows to include in the computation of the mean of
   *        the given column
   * @param col the column index to compute the mean value w.r.t. the given set
   *        of rows (relates to attribute
   *        <code>{@link #colIDs colIDs[col]}</code> of the corresponding
   *        database entries)
   * @return the mean value of the specified column over the specified rows
   */
  protected double meanOfCol(BitSet rows, int col) {
    Mean m = new Mean();
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      m.put(valueAt(i, col));
    }
    return m.getMean();
  }

  /**
   * Provides the mean of all entries in the submatrix as specified by a set of
   * columns and a set of rows.
   * 
   * @param rows the set of rows to include in the computation of the mean of
   *        the submatrix
   * @param cols the set of columns to include in the computation of the mean of
   *        the submatrix
   * @return the mean of all entries in the submatrix
   */
  protected double meanOfBicluster(BitSet rows, BitSet cols) {
    double sum = 0;
    for(int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      sum += meanOfRow(i, cols);
    }
    return sum / rows.cardinality();
  }

  /**
   * Provides the number of rows of the data matrix.
   * 
   * @return the number of rows of the data matrix
   */
  protected int getRowDim() {
    return this.rowIDs.size();
  }

  /**
   * Provides the number of columns of the data matrix.
   * 
   * @return the number of columns of the data matrix
   */
  protected int getColDim() {
    return colDim;
  }

  /**
   * Getter for database.
   * 
   * @return database
   */
  public Database getDatabase() {
    return database;
  }

  /**
   * Getter for the relation.
   * 
   * @return relation
   */
  public Relation<V> getRelation() {
    return relation;
  }
}