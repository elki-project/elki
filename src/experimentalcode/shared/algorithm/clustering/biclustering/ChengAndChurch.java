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
import de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering.AbstractBiclustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.BiclusterWithInversionsModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
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
 * <li>{@link #SEED_PARAM}</li>
 * </ul>
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
 * @author Erich Schubert
 * @param <V> a certain subtype of NumberVector - the data matrix is supposed to
 *        consist of currentRows where each row relates to an object of type V
 *        and the columns relate to the attribute values of these objects
 */
@Title("ChengAndChurch: A biclustering method on row- and column score base")
@Reference(authors = "Y. Cheng, G. M. Church", title = "Biclustering of expression data", booktitle = "Proc. 8th International Conference on Intelligent Systems for Molecular Biology (ISMB)")
public class ChengAndChurch<V extends NumberVector<?>> extends AbstractBiclustering<V, BiclusterWithInversionsModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ChengAndChurch.class);

  /**
   * The minimum number of columns that the database must have so that a removal
   * of columns is performed in {@link #multipleNodeDeletion()}.</p>
   * <p>
   * Just start deleting multiple columns when more than 100 columns are in the
   * data matrix.
   * </p>
   */
  private static final int MIN_COLUMN_REMOVE_THRESHOLD = 100;

  /**
   * The minimum number of rows that the database must have so that a removal of
   * rows is performed in {@link #multipleNodeDeletion()}.
   * <p>
   * Just start deleting multiple rows when more than 100 rows are in the data
   * matrix.
   * </p>
   * <!--
   * <p>
   * The value is set to 100 as this is not really described in the paper.
   * </p>
   * -->
   */
  private static final int MIN_ROW_REMOVE_THRESHOLD = 100;

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
  private long[] currentCols;

  /**
   * Indicates the current rows that belong to the current bicluster.
   */
  private long[] currentRows;

  /**
   * Keeps the position of the inverted rows belonging to the current bicluster.
   */
  private long[] invertedRows;

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

  private boolean useinverted = false;

  /**
   * Distribution to sample random replacement values from.
   */
  private Distribution dist;

  /**
   * Constructor.
   * 
   * @param delta Delta parameter: desired quality
   * @param alpha Alpha parameter: controls switching to single node deletion
   *        approach
   * @param n Number of clusters to detect
   * @param dist Distribution of random values to insert
   */
  public ChengAndChurch(double delta, double alpha, int n, Distribution dist) {
    super();
    this.delta = delta;
    this.alpha = alpha;
    this.n = n;
    this.dist = dist;
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
  public Clustering<BiclusterWithInversionsModel> biclustering() {
    this.maskedVals = new TLongDoubleHashMap(getRowDim() * getColDim(), .5f, -1, Double.NaN);

    Clustering<BiclusterWithInversionsModel> result = new Clustering<>("cheng-and-church", "Cheng and Church Biclustering");

    ModifiableDBIDs noise = DBIDUtil.newHashSet(relation.getDBIDs());

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Extracting Cluster", n, LOG) : null;
    for (int i = 0; i < n; i++) {
      this.reset();
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose("Initial residue: " + currentResidue);
      }
      this.multipleNodeDeletion();
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 2: " + currentResidue + " " + BitsUtil.cardinality(currentRows) + "x" + BitsUtil.cardinality(currentCols));
      }
      this.singleNodeDeletion();
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 1: " + currentResidue + " " + BitsUtil.cardinality(currentRows) + "x" + BitsUtil.cardinality(currentCols));
      }
      this.nodeAddition();
      if (LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 3: " + currentResidue + " " + BitsUtil.cardinality(currentRows) + "x" + BitsUtil.cardinality(currentCols));
      }
      this.maskMatrix(maskedVals, currentRows, currentCols, dist);
      BiclusterWithInversionsModel model = new BiclusterWithInversionsModel(colsBitsetToIDs(currentCols), rowsBitsetToIDs(invertedRows));
      final ArrayDBIDs cids = rowsBitsetToIDs(currentRows);
      noise.removeDBIDs(cids);
      result.addToplevelCluster(new Cluster<>(cids, model));

      if (LOG.isVerbose()) {
        LOG.verbose("Score of bicluster " + (i + 1) + ": " + this.currentResidue + "\n");
        LOG.verbose("Number of rows: " + BitsUtil.cardinality(currentRows) + "\n");
        LOG.verbose("Number of rows: " + cids.size() + "\n");
        LOG.verbose("Number of columns: " + BitsUtil.cardinality(currentCols) + ": " + BitsUtil.toString(currentCols) + "\n");
        LOG.verbose("Total number of masked values: " + maskedVals.size() + "\n");
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    // Add a noise cluster, full-dimensional.
    if (!noise.isEmpty()) {
      currentCols = BitsUtil.ones(getColDim());
      BiclusterWithInversionsModel model = new BiclusterWithInversionsModel(colsBitsetToIDs(currentCols), DBIDUtil.EMPTYDBIDS);
      result.addToplevelCluster(new Cluster<>(noise, true, model));
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
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
    columnMeans = new double[getColDim()];

    currentCols = BitsUtil.ones(getColDim());
    currentRows = BitsUtil.ones(getRowDim());
    invertedRows = BitsUtil.zero(getRowDim());
    updateValues(true);
  }

  /**
   * Algorithm 1 of Cheng and Church:
   * 
   * Remove single rows or columns.
   * 
   * Inverted rows are not supported in this method.
   */
  private void singleNodeDeletion() {
    int numr = (int) BitsUtil.cardinality(currentRows);
    int numc = (int) BitsUtil.cardinality(currentCols);
    // Assume that currentResidue is up to date!
    while (this.currentResidue > delta) {
      // Current maximum saving
      double max = 0.;
      int bestrow = -1, bestcol = -1;

      // Test rows
      if (numr > 2) {
        for (int rlpos = 0, rnum = 0; rlpos < currentRows.length; ++rlpos) {
          long rlong = currentRows[rlpos];
          // Fast skip blocks of 64 masked values.
          if (rlong == 0L) {
            rnum += Long.SIZE;
            continue;
          }
          for (int i = 0; i < Long.SIZE; ++i, rlong >>>= 1, rnum++) {
            if ((rlong & 1L) == 1L) {
              double rowResidue = computeRowResidue(rnum, false);
              if (max < rowResidue) {
                max = rowResidue;
                bestrow = rnum;
              }
            }
          }
        }
      }

      // Test columns:
      if (numc > 2) {
        for (int clpos = 0, cnum = 0; clpos < currentCols.length; ++clpos) {
          long clong = currentCols[clpos];
          // Fast skip blocks of 64 masked values.
          if (clong == 0L) {
            cnum += Long.SIZE;
            continue;
          }
          for (int i = 0; i < Long.SIZE; ++i, clong >>>= 1, cnum++) {
            if ((clong & 1L) == 1L) {
              double colResidue = computeColResidue(cnum);
              if (max < colResidue) {
                max = colResidue;
                bestcol = cnum;
              }
            }
          }
        }
      }

      if (bestcol >= 0) { // then override bestrow!
        BitsUtil.clearI(currentCols, bestcol);
        numc--;
      } else {
        assert (bestrow >= 0);
        BitsUtil.clearI(currentRows, bestrow);
        numr--;
      }
      // TODO: incremental update would be much faster!
      updateValues(false);
    }
  }

  //
  /**
   * Algorithm 2 of Cheng and Church.
   * 
   * Remove all rows and columns that reduce the residue by alpha.
   * 
   * Inverted rows are not supported in this method.
   */
  private void multipleNodeDeletion() {
    int numr = (int) BitsUtil.cardinality(currentRows);
    int numc = (int) BitsUtil.cardinality(currentCols);
    // Note: assumes that currentResidue = H(I,J)
    while (this.currentResidue > delta) {
      boolean modified = false;

      // Step 2: remove rows above threshold
      if (numr > MIN_ROW_REMOVE_THRESHOLD) {
        final double alphaResidue = alpha * currentResidue;
        // Compute row mean for each row i
        outer: for (int rlpos = 0, rnum = 0; rlpos < currentRows.length; ++rlpos) {
          long rlong = currentRows[rlpos];
          // Fast skip blocks of 64 masked values.
          if (rlong == 0L) {
            rnum += Long.SIZE;
            continue;
          }
          for (int i = 0; i < Long.SIZE; ++i, rlong >>>= 1, rnum++) {
            if ((rlong & 1L) == 1L) {
              if (computeRowResidue(rnum, false) > alphaResidue) {
                BitsUtil.clearI(currentRows, rnum);
                modified = true;
                numr--;
                if (numr < MIN_ROW_REMOVE_THRESHOLD) {
                  break outer;
                }
              }
            }
          }
        }

        // Step 3: update residue
        if (modified) {
          updateValues(false);
        }
      }

      // Step 4: remove columns above threshold
      if (numc > MIN_COLUMN_REMOVE_THRESHOLD) {
        final double alphaResidue = alpha * currentResidue;
        boolean colRemoved = false;
        // Compute row mean for each column j
        outer: for (int clpos = 0, cnum = 0; clpos < currentCols.length; ++clpos) {
          long clong = currentCols[clpos];
          // Fast skip blocks of 64 masked values.
          if (clong == 0L) {
            cnum += Long.SIZE;
            continue;
          }
          for (int i = 0; i < Long.SIZE; ++i, clong >>>= 1, cnum++) {
            if ((clong & 1L) == 1L) {
              if (computeColResidue(cnum) > alphaResidue) {
                BitsUtil.clearI(currentCols, cnum);
                modified = true;
                colRemoved = true;
                numc--;
                if (numc < MIN_COLUMN_REMOVE_THRESHOLD) {
                  break outer;
                }
              }
            }
          }
        }
        if (colRemoved) {
          updateValues(false);
        }
      }

      // Step 5: if nothing has been removed, try removing single nodes.
      if (!modified) {
        break;
        // Will be executed next in main loop, as per algorithm 4.
        // singleNodeDeletion();
      }
    }
  }

  /**
   * Algorithm 3 of Cheng and Church.
   * 
   * Try to re-add rows or columns that decrease the overall score.
   * 
   * Also try adding inverted rows.
   */
  private void nodeAddition() {
    updateValues(true);
    while (true) {
      boolean added = false;

      // Step 2: add columns
      for (int clpos = 0, cnum = 0; clpos < currentCols.length; ++clpos) {
        long clong = currentCols[clpos];
        // Fast skip blocks of 64 masked values.
        if (clong == 0xFFFFFFFFFFFFFFFFL) {
          cnum += Long.SIZE;
          continue;
        }
        for (int i = 0; i < Long.SIZE && cnum < columnMeans.length; ++i, clong >>>= 1, cnum++) {
          if ((clong & 1L) == 0L) {
            if (computeColResidue(cnum) <= currentResidue) {
              BitsUtil.setI(currentCols, cnum);
              added = true;
            }
          }
        }
      }

      // Step 3: recompute values
      if (added) {
        updateValues(true);
      }

      // Step 4: try adding rows.
      for (int rlpos = 0, rnum = 0; rlpos < currentRows.length; ++rlpos) {
        long rlong = currentRows[rlpos];
        // Fast skip blocks of 64 masked values.
        if (rlong == 0xFFFFFFFFFFFFFFFFL) {
          rnum += Long.SIZE;
          continue;
        }
        for (int i = 0; i < Long.SIZE && rnum < rowMeans.length; ++i, rlong >>>= 1, rnum++) {
          if ((rlong & 1L) == 0L) {
            if (computeRowResidue(rnum, false) <= currentResidue) {
              BitsUtil.setI(currentRows, rnum);
              added = true;
            }
          }
        }
      }

      // Step 5: try adding inverted rows.
      if (useinverted) {
        for (int rlpos = 0, rnum = 0; rlpos < currentRows.length && rnum < rowMeans.length; ++rlpos) {
          long rlong = currentRows[rlpos];
          // Fast skip blocks of 64 masked values.
          if (rlong == 0xFFFFFFFFFFFFFFFFL) {
            rnum += Long.SIZE;
            continue;
          }
          for (int i = 0; i < Long.SIZE && rnum < rowMeans.length; ++i, rlong >>>= 1, rnum++) {
            if ((rlong & 1L) == 0L) {
              if (computeRowResidue(rnum, true) <= currentResidue) {
                BitsUtil.setI(currentRows, rnum);
                BitsUtil.setI(invertedRows, rnum);
                added = true;
              }
            }
          }
        }
      }
      if (added) {
        updateValues(true);
      } else {
        break;
      }
    }
  }

  /**
   * Updates the mask with replacement values for all data in the given rows and
   * columns.
   * 
   * @param mask Mask to update.
   * @param rows Selected rows.
   * @param cols Selected columns.
   * @param replacement Distribution to sample replacement values from.
   */
  private void maskMatrix(TLongDoubleMap mask, long[] rows, long[] cols, Distribution replacement) {
    for (int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
      long rlong = rows[rlpos];
      // Fast skip blocks of 64 masked values.
      if (rlong == 0L) {
        rpos += Long.SIZE;
        continue;
      }
      for (int i = 0; i < Long.SIZE; ++i, ++rpos, rlong >>>= 1) {
        if ((rlong & 1L) == 1L) {
          for (int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
            long clong = cols[clpos];
            if (clong == 0L) {
              cpos += Long.SIZE;
              continue;
            }
            for (int j = 0; j < Long.SIZE; ++j, ++cpos, clong >>>= 1) {
              if ((clong & 1L) == 1L) {
                mask.put((((long) rpos) << 32) | cpos, replacement.nextRandom());
              }
            }
          }
        }
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
  private void updateValues(boolean updateall) {
    if (!updateall) {
      currentMean = updateRowAndColumnMeans(currentRows, currentCols, rowMeans, columnMeans, maskedVals);
    } else {
      currentMean = updateAllRowAndColumnMeans(currentRows, currentCols, rowMeans, columnMeans, maskedVals);
    }
    currentResidue = computeMeanSquaredDeviation(currentRows, currentCols, rowMeans, columnMeans, currentMean, maskedVals);
  }

  /**
   * Computes the <b>mean row residue</b> of the given <code>row</code>.
   * 
   * @param row The row who's residue should be computed.
   * @param rowinverted Indicates if the row should be considered inverted.
   * @return The row residue of the given <code>row</code>.
   */
  private double computeRowResidue(int row, boolean rowinverted) {
    Mean rowResidue = new Mean();
    for (int cpos = 0, clpos = 0; clpos < currentCols.length; ++clpos) {
      long clong = currentCols[clpos];
      if (clong == 0L) {
        cpos += Long.SIZE;
        continue;
      }
      for (int j = 0; j < Long.SIZE && cpos < columnMeans.length; ++j, ++cpos, clong >>>= 1) {
        if ((clong & 1L) == 1L) {
          final double rowMean = rowMeans[row];
          final double colMean = columnMeans[cpos];
          double val;
          if (!rowinverted) {
            val = valueAt(row, cpos) - rowMean - colMean + currentMean;
          } else {
            val = -valueAt(row, cpos) + rowMean - colMean + currentMean;
          }
          rowResidue.put(val * val);
        }
      }
    }
    return rowResidue.getMean();
  }

  /**
   * 
   * Computes the <b>mean column residue</b> of the given <code>col</code>.
   * 
   * @param col The column who's residue should be computed.
   * @return The row residue of the given <code>col</code>um.
   */
  private double computeColResidue(int col) {
    final double bias = columnMeans[col] - currentMean;

    Mean colResidue = new Mean();
    for (int rpos = 0, rlpos = 0; rlpos < currentRows.length; ++rlpos) {
      long rlong = currentRows[rlpos];
      // Fast skip blocks of 64 masked values.
      if (rlong == 0L) {
        rpos += Long.SIZE;
        continue;
      }
      for (int i = 0; i < Long.SIZE && rpos < rowMeans.length; ++i, ++rpos, rlong >>>= 1) {
        if ((rlong & 1L) == 1L) {
          double val = valueAt(rpos, col) - rowMeans[i] - bias;
          colResidue.put(val * val);
        }
      }
    }
    return colResidue.getMean();
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
    long key = (((long) row) << 32) | col;
    double v = maskedVals.get(key);
    if (v == v) { // i.e. NOT NaN!
      return v;
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
     * Parameter to specify the distribution of replacement values when masking
     * a cluster.
     */
    public static final OptionID DIST_ID = new OptionID("chengandchurch.replacement", "Distribution of replacement values when masking found clusters.");

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
     * Distribution of replacement values.
     */
    private Distribution dist;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter deltaP = new DoubleParameter(DELTA_ID);
      if (config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }
      deltaP.addConstraint(new GreaterEqualConstraint(0.));

      IntParameter nP = new IntParameter(N_ID, 1);
      nP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(nP)) {
        n = nP.intValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 1.);
      alphaP.addConstraint(new GreaterEqualConstraint(1.));
      if (config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      ObjectParameter<Distribution> distP = new ObjectParameter<>(DIST_ID, Distribution.class, UniformDistribution.class);
      if (config.grab(distP)) {
        dist = distP.instantiateClass(config);
      }
    }

    @Override
    protected ChengAndChurch<V> makeInstance() {
      return new ChengAndChurch<>(delta, alpha, n, dist);
    }
  }
}
