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

import gnu.trove.map.TLongDoubleMap;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.BiclusterModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Abstract class as a convenience for different biclustering approaches.
 * <p/>
 * The typically required values describing submatrices are computed using the
 * corresponding values within a database of NumberVectors.
 * <p/>
 * The database is supposed to present a data matrix with a row representing an
 * entry ({@link NumberVector}), a column representing a dimension (attribute)
 * of the {@link NumberVector}s.
 * 
 * @author Arthur Zimek
 * @param <V> a certain subtype of NumberVector - the data matrix is supposed to
 *        consist of rows where each row relates to an object of type V and the
 *        columns relate to the attribute values of these objects
 * @param <M> Cluster model type
 */
public abstract class AbstractBiclustering<V extends NumberVector<?>, M extends BiclusterModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>> {
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
    if (this.relation == null || this.relation.size() == 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    colDim = RelationUtil.dimensionality(relation);
    rowIDs = DBIDUtil.ensureArray(this.relation.getDBIDs());
    iter = rowIDs.iter();
    return biclustering();
  }

  /**
   * Run the actual biclustering algorithm.
   * <p/>
   * This method is supposed to be called only from the method
   * {@link #run(Database)}.
   * <p/>
   * If a bicluster is to be appended to the result, the methods
   * {@link #defineBicluster(BitSet,BitSet)} and
   * {@link #addBiclusterToResult(BiclusterModel)} should be used.
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
    for (int i = cols.nextSetBit(0); i >= 0; i = cols.nextSetBit(i + 1)) {
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
    for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      iter.seek(i);
      rowIDs.add(iter);
    }
    return rowIDs;
  }

  /**
   * Defines a Bicluster as given by the included rows and columns.
   * 
   * @param rows the rows included in the Bicluster
   * @param cols the columns included in the Bicluster
   * @return a Bicluster as given by the included rows and columns
   */
  protected Cluster<BiclusterModel> defineBicluster(BitSet rows, BitSet cols) {
    ArrayDBIDs rowIDs = rowsBitsetToIDs(rows);
    int[] colIDs = colsBitsetToIDs(cols);
    return new Cluster<>(rowIDs, new BiclusterModel(colIDs));
  }

  /**
   * Defines a Bicluster as given by the included rows and columns.
   * 
   * @param rows the rows included in the Bicluster
   * @param cols the columns included in the Bicluster
   * @return A Bicluster as given by the included rows and columns
   */
  protected Cluster<BiclusterModel> defineBicluster(long[] rows, long[] cols) {
    ArrayDBIDs rowIDs = rowsBitsetToIDs(rows);
    int[] colIDs = colsBitsetToIDs(cols);
    return new Cluster<>(rowIDs, new BiclusterModel(colIDs));
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
   * Update the row means and column means.
   * 
   * @param rows Masked rows
   * @param cols Masked columns
   * @param rowmeans Output array for row means
   * @param colmeans Output array for column means
   * @param mask Mask with substitution values
   * @return overall mean
   */
  protected double updateRowAndColumnMeans(long[] rows, long[] cols, double[] rowmeans, double[] colmeans, TLongDoubleMap mask) {
    Mean overall = new Mean(), rowmean = new Mean();
    Mean[] colmean = Mean.newArray(colmeans.length);
    // For efficiency, we manually iterate over the rows and column bitmasks.
    // This saves repeated shifting needed by the manual bit access.
    DBIDArrayIter iter = rowIDs.iter();
    outer: for (int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
      long rlong = rows[rlpos];
      // Fast skip blocks of 64 masked values.
      if (rlong == 0L) {
        rpos += Long.SIZE;
        iter.advance(Long.SIZE);
        continue;
      }
      for (int i = 0; i < Long.SIZE; ++i, ++rpos, rlong >>>= 1, iter.advance()) {
        if (!iter.valid()) {
          break outer;
        }
        if ((rlong & 1L) == 1L) {
          rowmean.reset();
          V vec = relation.get(iter);
          for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
            long clong = cols[clpos];
            if (clong == 0L) {
              cpos += Long.SIZE;
              continue;
            }
            for (int j = 0; j < Long.SIZE; ++j, ++cpos, clong >>>= 1) {
              if ((clong & 1L) == 1L) {
                double v = getMaskedValue(vec, cpos, mask, rpos);
                rowmean.put(v);
                colmean[cpos].put(v);
                overall.put(v);
              }
            }
          }
          rowmeans[rpos] = rowmean.getMean();
        }
      }
    }
    for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
      long clong = cols[clpos];
      if (clong == 0L) {
        cpos += Long.SIZE;
        continue;
      }
      for (int j = 0; j < Long.SIZE; ++j, ++cpos, clong >>>= 1) {
        if ((clong & 1L) == 1L) {
          colmeans[cpos] = colmean[cpos].getMean();
        }
      }
    }
    return overall.getMean();
  }

  /**
   * Update all row means and column means, only partially ignoring masks.
   * 
   * @param rows Masked rows
   * @param cols Masked columns
   * @param rowmeans Output array for row means
   * @param colmeans Output array for column means
   * @param mask Mask with substitution values
   * @return overall mean
   */
  protected double updateAllRowAndColumnMeans(long[] rows, long[] cols, double[] rowmeans, double[] colmeans, TLongDoubleMap mask) {
    Mean overall = new Mean(), rowmean = new Mean();
    Mean[] colmean = Mean.newArray(colmeans.length);
    // For efficiency, we manually iterate over the rows and column bitmasks.
    // This saves repeated shifting needed by the manual bit access.
    DBIDArrayIter iter = rowIDs.iter();
    outer: for (int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
      long rlong = rows[rlpos];
      // Fast skip blocks of 64 masked values.
      for (int i = 0; i < Long.SIZE && rpos < rowmeans.length; ++i, ++rpos, rlong >>>= 1, iter.advance()) {
        if (!iter.valid()) {
          break outer;
        }
        boolean rselected = ((rlong & 1L) == 1L);
        rowmean.reset();
        V vec = relation.get(iter);
        for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
          long clong = cols[clpos];
          for (int j = 0; j < Long.SIZE && cpos < colmeans.length; ++j, ++cpos, clong >>>= 1) {
            boolean cselected = ((clong & 1L) == 1L);
            double v = getMaskedValue(vec, cpos, mask, rpos);
            if (cselected) {
              rowmean.put(v);
            }
            if (rselected) {
              colmean[cpos].put(v);
            }
            if (rselected && cselected) {
              overall.put(v);
            }
          }
          rowmeans[rpos] = rowmean.getMean();
        }
      }
    }
    for (int cpos = 0; cpos < colmean.length; ++cpos) {
      colmeans[cpos] = colmean[cpos].getMean();
    }
    return overall.getMean();
  }

  /**
   * Compute the mean square residue.
   * 
   * @param rows Masked rows
   * @param cols Masked columns
   * @param rowmeans Output array for row means
   * @param colmeans Output array for column means
   * @param allmean overall mean
   * @param mask Mask with substitution values
   * @return mean squared residue
   */
  protected double computeMeanSquaredDeviation(long[] rows, long[] cols, double[] rowmeans, double[] colmeans, double allmean, TLongDoubleMap mask) {
    Mean msr = new Mean();
    // For efficiency, we manually iterate over the rows and column bitmasks.
    // This saves repeated shifting needed by the manual bit access.
    DBIDArrayIter iter = rowIDs.iter();
    outer: for (int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
      long rlong = rows[rlpos];
      // Fast skip blocks of 64 masked values.
      if (rlong == 0L) {
        rpos += Long.SIZE;
        iter.advance(Long.SIZE);
        continue;
      }
      for (int i = 0; i < Long.SIZE; ++i, ++rpos, rlong >>>= 1, iter.advance()) {
        if (!iter.valid()) {
          break outer;
        }
        if ((rlong & 1L) == 1L) {
          V vec = relation.get(iter);
          for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
            long clong = cols[clpos];
            if (clong == 0L) {
              cpos += Long.SIZE;
              continue;
            }
            for (int j = 0; j < Long.SIZE; ++j, ++cpos, clong >>>= 1) {
              if ((clong & 1L) == 1L) {
                double v = getMaskedValue(vec, cpos, mask, rpos);
                v = v - rowmeans[rpos] - colmeans[cpos] + allmean;
                msr.put(v * v);
              }
            }
          }
        }
      }
    }
    return msr.getMean();
  }

  /**
   * Get masked values instead of actual scores.
   * 
   * @param vec Vector from database
   * @param cpos Column position
   * @param mask Mask
   * @param rpos Row position
   * @return Value from mask or vector.
   */
  protected double getMaskedValue(V vec, int cpos, TLongDoubleMap mask, int rpos) {
    if (mask != null) {
      double v = mask.get((((long)rpos) << 32) | cpos);
      if (v == v) { // NOT TRUE for NaN values.
        return v;
      }
    }
    return vec.doubleValue(cpos);
  }

  /**
   * Convert a bitset into integer column ids.
   * 
   * @param cols
   * @return integer column ids
   */
  protected int[] colsBitsetToIDs(long[] cols) {
    int[] colIDs = new int[(int) BitsUtil.cardinality(cols)];
    int colsIndex = 0;
    for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
      long clong = cols[clpos];
      if (clong == 0L) {
        cpos += Long.SIZE;
        continue;
      }
      for (int j = 0; j < Long.SIZE; ++j, ++cpos, clong >>>= 1) {
        if ((clong & 1L) == 1L) {
          colIDs[colsIndex] = cpos;
          ++colsIndex;
        }
      }
    }
    return colIDs;
  }

  /**
   * Convert a bitset into integer row ids.
   * 
   * @param rows
   * @return integer row ids
   */
  protected ArrayDBIDs rowsBitsetToIDs(long[] rows) {
    ArrayModifiableDBIDs rowIDs = DBIDUtil.newArray((int) BitsUtil.cardinality(rows));
    DBIDArrayIter iter = this.rowIDs.iter();
    outer: for (int rlpos = 0; rlpos < rows.length; ++rlpos) {
      long rlong = rows[rlpos];
      // Fast skip blocks of 64 masked values.
      if (rlong == 0L) {
        iter.advance(Long.SIZE);
        continue;
      }
      for (int i = 0; i < Long.SIZE; ++i, rlong >>>= 1, iter.advance()) {
        if (!iter.valid()) {
          break outer;
        }
        if ((rlong & 1L) == 1L) {
          rowIDs.add(iter);
        }
      }
    }
    return rowIDs;
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
