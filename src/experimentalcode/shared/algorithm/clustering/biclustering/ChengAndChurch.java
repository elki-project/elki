package experimentalcode.shared.algorithm.clustering.biclustering;

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
import gnu.trove.map.hash.TLongDoubleHashMap;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering.AbstractBiclustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.BiclusterWithInverted;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * <b>TODO: Check if implementation for inverted rows is valid</b><br/>
 * Provides a biclustering algorithm which deletes or inserts
 * currentRows/columns dependent on their score and finds biclusters with
 * correlated values.</p>
 * <p>
 * Parameters:
 * <ul>
 * <li>{@link #DELTA_PARAM}</li>
 * <li>{@link #ALPHA_PARAM}</li>
 * <li>{@link #N_PARAM}</li>
 * <li>{@link #BEGIN_PARAM}</li>
 * <li>{@link #END_PARAM}</li>
 * <li>{@link #MISSING_PARAM}</li>
 * <li>{@link #MULTIPLE_ADDITION_PARAM} ???</li>
 * <li>{@link #SEED_PARAM}</li>
 * </ul>
 * </p>
 * <p>
 * Implementation details: In each iteration a single bicluster is found. The
 * properties of the current bicluster are saved in the fields:
 * {@link #currentRows}, {@link #currentCols}, {@link #invertedRows}
 * {@link #currentResidue}, {@link #currentMean}, {@link #rowMeans} and
 * {@link #columnMeans}.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * Y. Cheng and G. M. Church. Biclustering of expression data. In Proceedings of
 * the 8th International Conference on Intelligent Systems for Molecular Biology
 * (ISMB), San Diego, CA, 2000.
 * </p>
 * 
 * @author Noemi Andor
 * @param <V> a certain subtype of NumberVector - the data matrix is supposed to
 *        consist of currentRows where each row relates to an object of type V
 *        and the columns relate to the attribute values of these objects
 */
@Title("ChengAndChurch: A biclustering method on row- and column score base")
@Description("Finding correlated values in a subset of currentRows and a subset of columns")
@Reference(authors = "Y. Cheng and G. M. Church", title = "Biclustering of expression data", booktitle = "Proceedings of the 8th International Conference on Intelligent Systems for Molecular Biology (ISMB), San Diego, CA, 2000")
public class ChengAndChurch<V extends NumberVector<?>> extends AbstractBiclustering<V, BiclusterWithInverted<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ChengAndChurch.class);

  /**
   * The minimum number of columns that the database must have so that a removal
   * of columns is performed in {@link #multipleNodeDeletion()}.</p>
   * <p>
   * Just start deleting multiple columns when more than 100 columns are in the
   * datamatrix.
   * </p>
   */
  private static final int MIN_COLUMN_REMOVE_THRESHOLD = 100;

  /**
   * The minimum number of rows that the database must have so that a removal of
   * rowss is performed in {@link #multipleNodeDeletion()}.
   * <p>
   * Just start deleting multiple rows when more than 100 rows are in the
   * datamatrix.
   * </p>
   * <!--
   * <p>
   * The value is set to 100 as this is not really described in the paper.
   * </p>
   * -->
   */
  private static final int MIN_ROW_REMOVE_THRESHOLD = 100;

  /**
   * Default value for {@link #MULTIPLE_ADDITION_PARAM}
   */
  private static final int DEFAULT_MULTIPLE_ADDITION = 1;

  /**
   * Threshold for the score ({@link #DELTA_PARAM}).
   */
  private double delta;

  /**
   * The parameter for multiple node deletion.</p>
   * <p>
   * It is used to magnify the {@link #delta} value in the
   * {@link #multipleNodeDeletion()} method.
   * </p>
   */
  private double alpha;

  /**
   * Number of biclusters to be found.
   */
  private int n;

  /**
   * Mean of the current bicluster.
   */
  private double currentMean;

  /**
   * The current bicluster score (mean squared residue).
   */
  private double currentResidue;

  /**
   * Indicates the columns that belong to the current bicluster.
   */
  private BitSet currentCols;

  /**
   * Indicates the current rows that belong to the current bicluster.
   */
  private BitSet currentRows;

  /**
   * All row means of the current bicluster.</p>
   * <p>
   * A row mean is the (arithmetic) mean of the values defined on the current
   * columns in a specific row (the key of the map). The value of the map is the
   * row mean.
   * </p>
   */
  private double[] rowMeans;

  /**
   * All column means of the current bicluster.</p>
   * <p>
   * A column mean is the (arithmetic) mean of the values defined on the current
   * rows in a specific column (the key of the map). The value of the map is the
   * column mean.
   * </p>
   */
  private double[] columnMeans;

  /**
   * Keeps the position of the inverted rows belonging to the current bicluster.
   */
  // TODO: Check if implementation of invertedRows is valid... especially in
  // mean computation, ...
  private BitSet invertedRows = new BitSet();

  /**
   * Lower threshold for random maskedValues.
   */
  private double minMissingValue;

  /**
   * Upper threshold for random maskedValues.
   */
  private double maxMissingValue;

  /**
   * <p>
   * Keeps track of all masked values. A masked value is occupied by a
   * previously found bicluster and thus populated with random noise so that a
   * new iteration will not detect the same cluster again.
   * </p>
   * <p>
   * The key of the map is an {@link IntIntPair} that contains the row and
   * column. The value of the map represents the new value that is randomly
   * distributed. The original value is still in the database but we bypass the
   * {@link AbstractBiclustering#valueAt(int, int)} by overriding it so the new
   * {@link #valueAt(int, int)} will return the masked value instead of the
   * original. The masked values will be between {@link #minMissingValue} and
   * {@link #maxMissingValue}.
   * </p>
   */
  private TLongDoubleMap maskedVals;

  /**
   * <p>
   * Keeps track of all values that are missing in the database (that are
   * <code>null</code>).
   * </p>
   * <p>
   * Depends on the {@link Database} implementation used. For every key that
   * represents a row and a column the value is returned when calling
   * {@link #valueAt(int, int)}.
   * </p>
   * 
   */
  private TLongDoubleMap missingValues;

  private int numberOfAddition = DEFAULT_MULTIPLE_ADDITION;

  /**
   * Value that indicates a missing value (e.g. NaN)
   */
  private double missing;

  /**
   * A random for generating values for the missing or masking
   * currentRows/columns.
   */
  private Random random;

  public ChengAndChurch(double delta, double alpha, int n, double minMissingValue, double maxMissingValue, int numberOfAddition, double missing, RandomFactory rnd) {
    super();
    this.delta = delta;
    this.alpha = alpha;
    this.n = n;
    this.minMissingValue = minMissingValue;
    this.maxMissingValue = maxMissingValue;
    this.numberOfAddition = numberOfAddition;
    this.missing = missing;
    this.random = rnd.getRandom();
  }

  /**
   * Initiates this algorithm and runs {@link #n} iterations to find the
   * {@link #n} biclusters.</p>
   * <p>
   * It first checks if some missing (<code>NULL</code>) values are in the
   * database and masks them ({@link #replaceMissingValues()}).<br />
   * For each iteration it starts with the complete database as subspace cluster
   * and performs multiple row and column deletions (
   * {@link #multipleNodeDeletion()}) if more than 100 rows or columns are in
   * the datamatrix, otherwise {@link #singleNodeDeletion()} is performed until
   * the {@link #delta} value is reached. Then it calls the
   * {@link #nodeAddition()} and finally masks the found bicluster (
   * {@link #maskMatrix()}). A reset operation is called after each iteration to
   * start with the complete database as starting subspace cluster again (
   * {@link #reset()}).
   * </p>
   * 
   * @return
   */
  @Override
  public Clustering<BiclusterWithInverted<V>> biclustering() {
    this.maskedVals = new TLongDoubleHashMap(getRowDim() * getColDim(), .5f, -1, Double.NaN);

    Clustering<BiclusterWithInverted<V>> result = new Clustering<>("cheng-and-church", "Cheng and Church Biclustering");
    this.replaceMissingValues();
    for (int i = 0; i < n; i++) {
      this.reset();
      this.multipleNodeDeletion();
      this.nodeAddition();
      this.maskMatrix();
      BiclusterWithInverted<V> bicluster = new BiclusterWithInverted<>(rowsBitsetToIDs(currentRows), colsBitsetToIDs(currentCols), getRelation());
      bicluster.setInvertedRows(rowsBitsetToIDs(invertedRows));
      addBiclusterToResult(result, bicluster);

      if (LOG.isVerbose()) {
        LOG.verbose("Score of bicluster" + (i + 1) + ": " + this.currentResidue);
        LOG.verbose("Number of rows: " + currentRows.cardinality());
        LOG.verbose("Number of columns: " + currentCols.cardinality());
        LOG.verbose("Total number of masked values: " + maskedVals.size() + "\n");
      }
    }
    return result;
  }

  /**
   * <p>
   * Resets the values for the next iteration to start.
   * </p>
   * <p>
   * These are {@link #rowMeans}, {@link #columnMeans}, {@link #currentMean},
   * {@link #currentResidue}, {@link #invertedRows}. It sets the
   * {@link #currentRows} and {@link #currentCols} from 0 to
   * {@link #getRowDim()} ({@link #getColDim()} resp.).<br/>
   * It calls {@link #updateValues()} at the end.
   * </p>
   */
  private void reset() {
    currentResidue = 0.;
    currentMean = 0.;

    rowMeans = new double[getRowDim()];
    Arrays.fill(rowMeans, Double.NaN); // not computed yet.
    columnMeans = new double[getColDim()];
    Arrays.fill(columnMeans, Double.NaN); // not computed yet.

    invertedRows.clear();

    currentCols = new BitSet(getColDim());
    currentCols.set(0, getColDim());
    currentRows = new BitSet(getRowDim());
    currentRows.set(0, getRowDim());
    updateValues();
  }

  //
  /**
   * Algorithm 2 of Cheng and Church.
   * 
   * Remove all rows and columns that reduce the residue by alpha.
   */
  private void multipleNodeDeletion() {
    // If something has been removed:
    boolean removed = false;

    // Note: assumes that currentResidue = H(I,J)
    BitSet unionRows = new BitSet(getRowDim());
    while (this.currentResidue > delta) {
      removed = false;
      // TODO: Proposal in paper: use an adaptive alpha based on the new scores
      // of the current bicluster.
      double alphaResidue = alpha * currentResidue;
      // TODO: Maybe use the row dim of the current cluster instead of the dim
      // of the database?

      // Step 2: remove rows above threshold
      if (getRowDim() > MIN_ROW_REMOVE_THRESHOLD) {
        // Compute row mean for each row i
        unionRows.clear();
        unionRows.or(currentRows);
        unionRows.or(invertedRows);
        // BitSet invertedRowsToRemove = new ArrayList<>();
        for (int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
          if (computeRowResidue(i, false) > alphaResidue) {
            currentRows.clear(i);
            invertedRows.clear(i);
            removed = true;
          }
        }
        // TODO: same for inverted rows.

        // Step 3: update residue
        if (removed) {
          updateValues();
          alphaResidue = alpha * currentResidue;
        }
      }

      // Step 4: remove columns above threshold
      if (getColDim() > MIN_COLUMN_REMOVE_THRESHOLD) {
        boolean colRemoved = false;
        // Compute row mean for each column j
        for (int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
          if (computeColResidue(j) > alphaResidue) {
            currentCols.clear(j);
            removed = true;
            colRemoved = true;
          }
        }
        if (colRemoved) {
          updateValues();
        }
      }

      // Step 5: if nothing has been removed, try removing single nodes.
      if (!removed) {
        singleNodeDeletion();
      }
    }
  }

  /**
   * Algorithm 1 of Cheng and Church:
   * 
   * Find a single row or column to remove.
   */
  private void singleNodeDeletion() {
    // Assume that currentResidue is up to date!
    while (this.currentResidue > delta) {
      // Current maximum saving
      double max = 0.;
      int bestrow = -1, bestcol = -1;
      BitSet unionRows = getUnionRows();

      // Test rows
      for (int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
        double rowResidue = computeRowResidue(i, false);
        if (max < rowResidue) {
          max = rowResidue;
          bestrow = i;
        }
        // TODO: add support for inverted?
      }

      // Test columns:
      for (int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
        double colResidue = computeColResidue(j);
        if (max < colResidue) {
          max = colResidue;
          bestcol = j;
        }
      }

      if (bestcol > -1) { // then override bestrow!
        currentCols.clear(bestcol);
      } else {
        assert (bestrow >= 0);
        currentRows.clear(bestrow);
        invertedRows.clear(bestrow);
      }
      // TODO: update only as much as necessary!
      updateValues();
    }
  }

  /**
   * <p>
   * Adds alternating rows and columns so that the {@link #currentResidue} will
   * decrease. This is done {@link #MULTIPLE_ADDITION_PARAM} times if the
   * parameter is set. Otherwise, {@link #DEFAULT_MULTIPLE_ADDITION} times.
   * </p>
   * <p>
   * Also addes the <b>inverse</b> of a row.
   * </p>
   */
  private void nodeAddition() {
    boolean added = true;

    while (added && numberOfAddition > 0) {
      added = false;
      numberOfAddition--;
      // Compute row mean for each col j
      for (int j = currentCols.nextClearBit(0); j < getColDim(); j = currentCols.nextClearBit(j + 1)) {
        if (computeColResidue(j) <= currentResidue) {
          currentCols.set(j);
          added = true;
        }
      }
      if (added) {
        updateValues();
      }

      // Compute row mean for each row i
      // BitSet unionRows = new BitSet();
      // unionRows.or(currentRows);
      // unionRows.or(invertedRows);

      for (int i = currentRows.nextClearBit(0); i < getRowDim(); i = currentRows.nextClearBit(i + 1)) {
        if (computeRowResidue(i, false) <= currentResidue) {
          currentRows.set(i);
          invertedRows.clear(i);
          added = true;
        }
      }
      for (int i = invertedRows.nextClearBit(0); i < getRowDim(); i = invertedRows.nextClearBit(i + 1)) {
        if (computeRowResidue(i, true) <= currentResidue) {
          invertedRows.set(i);
          currentRows.clear(i);
          added = true;
        }
      }
      if (added) {
        updateValues();
      }
    }
  }

  /**
   * Fills the missing values with random values ranging from
   * {@link #minMissingValue} to {@link #maxMissingValue} in a uniform
   * manner.</p>
   * <p>
   * It does nothing if {@link #MISSING_PARAM} is not set as it is not clear
   * what values to treat as missing values.
   * </p>
   * 
   * @see #BEGIN_PARAM
   * @see #END_PARAM
   */
  private void replaceMissingValues() {
    if (this.missingValues == null) {
      this.missingValues = new TLongDoubleHashMap(getRowDim(), .5f, -1, Double.NaN);
    }
    final double range = maxMissingValue - minMissingValue;
    for (int i = 0; i < getRowDim(); i++) {
      for (int j = 0; j < getColDim(); j++) {
        if (super.valueAt(i, j) == missing) {
          missingValues.put((i << 32) | j, minMissingValue + random.nextDouble() * range);
        }
      }
    }
  }

  /**
   * Masks all values in {@link #currentRows} UNION {@link #invertedRows} and
   * {@link #currentCols} belonging to the current bicluster.</p>
   * <p>
   * The masking values range from {@link #minMissingValue} to
   * {@link #maxMissingValue} in a uniform manner.
   * </p>
   * 
   * @see #BEGIN_PARAM
   * @see #END_PARAM
   */
  private void maskMatrix() {
    BitSet unionRows = getUnionRows();
    for (int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      for (int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
        maskedVals.put((i << 32) | j, minMissingValue + random.nextDouble() * (maxMissingValue - minMissingValue));
      }
    }
  }

  /**
   * Updates the {@link #currentResidue}, {@link #currentMean} and calls
   * {@link #updateAllColMeans()} and {@link #updateAllRowMeans()}. </p>
   * <p>
   * It uses the {@link #currentRows} and {@link #currentCols} for that purpose.
   * </p>
   */
  private void updateValues() {
    BitSet unionRows = getUnionRows();
    currentMean = meanOfSubmatrix(unionRows, currentCols);
    updateAllRowMeans();
    updateAllColMeans();
    currentResidue = computeMeanSquaredResidue(unionRows, currentCols);
  }

  /**
   * Computes all row means of the current bicluster ({@link #currentRowMeans}
   * UNION {@link #invertedRows} and {@link #currentColMeans}).
   * 
   * <!-- @param currentRows The currentRows of the sub matrix.
   * 
   * @param currentCols The columns of the sub matrix.
   * @return Returns a map that contains a row mean for every specified row in
   *         <code>currentRows</code>. -->
   */
  private void updateAllRowMeans() {
    this.rowMeans = new double[getRowDim()];
    Arrays.fill(rowMeans, Double.NaN);

    BitSet unionRows = getUnionRows();
    for (int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      rowMeans[i] = meanOfRow(i, currentCols);
    }
    // return rowMeans;
  }

  /**
   * Computes all column means of the current bicluster (
   * {@link #currentRowMeans} and {@link #currentColMeans}). <!--
   * 
   * @param currentRows The currentRows of the sub matrix.
   * @param currentCols The columns of the sub matrix.
   * @return Returns a map that contains a column mean for every specified
   *         column in <code>currentCols</code>. -->
   */
  private void updateAllColMeans() {
    this.columnMeans = new double[getColDim()];
    Arrays.fill(columnMeans, Double.NaN);

    BitSet unionRows = getUnionRows();
    for (int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
      columnMeans[j] = meanOfCol(unionRows, j);
    }
  }

  /**
   * Calculates the score (= mean squared residue) of the bicluster.</p> It uses
   * {@link #rowMeans}, {@link #columnMeans} and the {@link #currentMean}.
   * 
   * @param rows A BitSet that specifies the current rows of the bicluster
   *        (including the {@link #invertedRows}).
   * @param cols A BitSet that specifies the columns of the bicluster.
   * 
   * @return Returns the score (mean squared residue) of the given bicluster.
   */
  private double computeMeanSquaredResidue(BitSet rows, BitSet cols) {
    double msr = 0.0;
    for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
        // TODO: Handling of inverted rows?
        double val = valueAt(i, j) - rowMeans[i] - columnMeans[j] + currentMean;
        msr += (val * val);
      }
    }
    msr /= (rows.cardinality() * cols.cardinality());
    return msr;
  }

  /**
   * Computes the mean of the bicluster that spans over the given rows and
   * columns. Uses the custom {@link #valueAt(int, int)} method.</p>
   * 
   * @param cols A {@link BitSet} that indicates which columns should be used to
   *        compute the mean.
   * @param rows A {@link BitSet} that indicates which rows should be used to
   *        compute the mean. (Including {@link #invertedRows}) <!-- @param
   *        addition A flag that indicates if the mean should be computed in the
   *        addition mode ( <code>true</code>) or not (<code>false</code>). -->
   * @return Returns the mean of the bicluster at the given current rows and
   *         columns.
   * 
   */
  private double meanOfSubmatrix(BitSet rows, BitSet cols) {
    double sum = 0.0;
    for (int i = rows.nextSetBit(0); i >= 0; i = rows.nextSetBit(i + 1)) {
      for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
        sum += valueAt(i, j);
      }
    }
    return sum / (rows.cardinality() * cols.cardinality());
  }

  /**
   * Computes the <b>mean row residue</b> of the given <code>row</code>.
   * 
   * @param row The row who's residue should be computed.
   * @param rowinverted Indicates if the row should be considered inverted.
   * @return The row residue of the given <code>row</code>.
   */
  private double computeRowResidue(int row, boolean rowinverted) {
    double rowResidue = 0.;
    for (int j = currentCols.nextSetBit(0); j >= 0; j = currentCols.nextSetBit(j + 1)) {
      double rowMean = cachedRowMean(row);
      double colMean = cachedColumnMean(j);
      double val = 0.;
      if (!rowinverted) {
        val = valueAt(row, j) - rowMean - colMean + currentMean;
      } else {
        val = -valueAt(row, j) + rowMean - colMean + currentMean;
      }
      rowResidue += (val * val);
    }
    return (rowResidue / currentCols.cardinality());
  }

  /**
   * Compute the union of positive and inverted rows.
   * 
   * @return
   */
  protected BitSet getUnionRows() {
    BitSet unionRows = new BitSet(getRowDim());
    unionRows.or(currentRows);
    unionRows.or(invertedRows);
    return unionRows;
  }

  protected double cachedColumnMean(int j) {
    double colMean = columnMeans[j];
    if (Double.isNaN(colMean)) {
      BitSet unionRows = getUnionRows();
      colMean = super.meanOfCol(unionRows, j);
      columnMeans[j] = colMean;
    }
    return colMean;
  }

  protected double cachedRowMean(int row) {
    double rowMean = rowMeans[row];
    if (Double.isNaN(rowMean)) {
      rowMean = super.meanOfRow(row, currentCols);
      rowMeans[row] = rowMean;
    }
    return rowMean;
  }

  /**
   * 
   * Computes the <b>mean column residue</b> of the given <code>col</code>.
   * 
   * @param col The column who's residue should be computed.
   * @return The row residue of the given <code>col</code>um.
   */
  private double computeColResidue(int col) {
    BitSet unionRows = getUnionRows();

    final double bias = cachedColumnMean(col) + currentMean;

    double colResidue = 0.;
    for (int i = unionRows.nextSetBit(0); i >= 0; i = unionRows.nextSetBit(i + 1)) {
      double val = valueAt(i, col) - cachedRowMean(i) - bias;
      colResidue += val * val;
    }
    return colResidue / unionRows.cardinality();
  }

  /**
   * Returns the newly generated value at the specified position if this
   * position has been masked (see {@link #maskedVals}) or that this position is
   * missing ({@link #missingValues}). Otherwise the value in the database is
   * returned. </p>
   * <p>
   * Overrides {@link AbstractBiclustering#valueAt(int, int)}
   * </p>
   * 
   * @param row The row.
   * @param col The column.
   * @return Returns the fake value if the position (given by row and col) has
   *         been masked or is missing. Otherwise the original value in the
   *         database is returned.
   */
  @Override
  protected double valueAt(int row, int col) {
    long key = (row << 32) | col;
    if (maskedVals.containsKey(key)) {
      return maskedVals.get(key);
    }
    if (missingValues.containsKey(key)) {
      return missingValues.get(key);
    }
    return super.valueAt(row, col);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter to specify the seed for the random that creates the missing
     * values.
     * <p>
     * Default value: 0
     * </p>
     * <p>
     * Key: {@code -chengandchurch.random}
     * </p>
     */
    public static final OptionID SEED_ID = new OptionID("chengandchurch.random", "Seed for the random that creates the missing values.");

    /**
     * Threshold value to determine the maximal acceptable score (mean squared
     * residue) of a bicluster.
     * <p/>
     * Key: {@code -chengandchurch.delta}
     * </p>
     */
    public static final OptionID DELTA_ID = new OptionID("chengandchurch.delta", "Threshold value to determine the maximal acceptable score (mean squared residue) of a bicluster.");

    /**
     * Parameter for multiple node deletion to accelerate the algorithm. (&gt;=
     * 1)
     * <p/>
     * Key: {@code -chengandchurch.alpha}
     * </p>
     */
    public static final OptionID ALPHA_ID = new OptionID("chengandchurch.alpha", "Parameter for multiple node deletion to accelerate the algorithm.");

    /**
     * Number of biclusters to be found.
     * <p/>
     * Default value: 1
     * </p>
     * <p/>
     * Key: {@code -chengandchurch.n}
     * </p>
     */
    public static final OptionID N_ID = new OptionID("chengandchurch.n", "The number of biclusters to be found.");

    /**
     * Lower limit for the masking and missing values.
     * <p/>
     * Key: {@code -chengandchurch.begin}
     * </p>
     */
    public static final OptionID BEGIN_ID = new OptionID("chengandchurch.begin", "The lower limit for the masking and missing values. Must be smaller than the upper limit.");

    /**
     * Upper limit for the masking and missing values.
     * <p/>
     * Key: {@code -chengandchurch.end}
     * </p>
     */
    public static final OptionID END_ID = new OptionID("chengandchurch.end", "The upper limit for the masking and missing values.");

    /**
     * A parameter to indicate what value the missing values in the database
     * have.</p>
     * <p>
     * If a value is missing in the database, you have to give them a value
     * (e.g. -10000) and specify -10000 as this parameter.
     * </p>
     * <p>
     * The missing values in database will be replaced by a random generated
     * value in the range between {@link #BEGIN_PARAM} and {@link #END_PARAM}.
     * The missing values will be uniform distributed. Note that the random
     * values are <code>double</code> and not <code>int</code>.
     * </p>
     * <p>
     * Default: Replace Double.NaN values.
     * <p/>
     * Key: {@code -chengandchurch.missing}
     * </p>
     */
    public static final OptionID MISSING_ID = new OptionID("chengandchurch.missing", "Value indicating missing values. The default is NaN, but you can use this to indicate that e.g. -1 is invalid.");

    /**
     * Parameter to indicate how many times an addition should be performed in
     * each of the <code>n</code> iterations.</p>
     * <p>
     * A greater value will result in a more accurate result but will require
     * more time.
     * </p>
     * <p/>
     * Default value: 1 ({@value #DEFAULT_MULTIPLE_ADDITION})
     * </p>
     * <p/>
     * Key: {@code -chengandchurch.multipleAddition}
     * </p>
     */
    public static final OptionID MULTIPLE_ADDITION_ID = new OptionID("chengandchurch.multipleAddition", "Indicates how many times the algorithm to add Nodes should be performed.");

    /**
     * Random factory.
     */
    private RandomFactory rnd;

    /**
     * Threshold for the score ({@link #DELTA_PARAM}).
     */
    private double delta;

    /**
     * The parameter for multiple node deletion.</p>
     * <p>
     * It is used to magnify the {@link #delta} value in the
     * {@link #multipleNodeDeletion()} method.
     * </p>
     */
    private double alpha;

    /**
     * Number of biclusters to be found.
     */
    private int n;

    /**
     * Lower threshold for random maskedValues.
     */
    private double minMissingValue;

    /**
     * Upper threshold for random maskedValues.
     */
    private double maxMissingValue;

    /**
     * FIXME: document
     */
    private int numberOfAddition = DEFAULT_MULTIPLE_ADDITION;

    /**
     * FIXME: document
     */
    private double missing = Double.NaN;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter randomP = new RandomParameter(SEED_ID);
      if (config.grab(randomP)) {
        rnd = randomP.getValue();
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID);
      if (config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }
      deltaP.addConstraint(new GreaterEqualConstraint(0.));

      IntParameter nP = new IntParameter(N_ID);
      nP.setDefaultValue(1);
      nP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(nP)) {
        n = nP.intValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 1.);
      alphaP.addConstraint(new GreaterEqualConstraint(1.));
      if (config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      DoubleParameter beginP = new DoubleParameter(BEGIN_ID);
      if (config.grab(beginP)) {
        minMissingValue = beginP.doubleValue();
      }

      DoubleParameter endP = new DoubleParameter(END_ID);
      if (config.grab(endP)) {
        maxMissingValue = endP.doubleValue();
      }
      if (minMissingValue > maxMissingValue) {
        config.reportError(new WrongParameterValueException(beginP, "The minimum value for missing values is larger than the maximum value", "Minimum value: " + minMissingValue + "  maximum value: " + maxMissingValue));
      }

      IntParameter missingP = new IntParameter(MISSING_ID);
      missingP.setOptional(true);
      if (config.grab(missingP)) {
        missing = missingP.intValue();
      }

      IntParameter multiaddP = new IntParameter(MULTIPLE_ADDITION_ID, DEFAULT_MULTIPLE_ADDITION);
      multiaddP.setOptional(true);
      multiaddP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(multiaddP)) {
        numberOfAddition = multiaddP.getValue();
      }
    }

    @Override
    protected ChengAndChurch<V> makeInstance() {
      return new ChengAndChurch<>(delta, alpha, n, minMissingValue, maxMissingValue, numberOfAddition, missing, rnd);
    }
  }
}
