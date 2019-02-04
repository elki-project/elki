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
package de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.BiclusterWithInversionsModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Cheng and Church biclustering.
 * <p>
 * Reference:
 * <p>
 * Y. Cheng and G. M. Church.<br>
 * Biclustering of expression data.<br>
 * Proc. 8th Int. Conf. on Intelligent Systems for Molecular Biology (ISMB)
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - - - BiclusterCandidate
 *
 * @param <V> Vector type.
 */
@Reference(authors = "Y. Cheng, G. M. Church", //
    title = "Biclustering of expression data", //
    booktitle = "Proc. 8th Int. Conf. on Intelligent Systems for Molecular Biology (ISMB)", //
    url = "http://www.aaai.org/Library/ISMB/2000/ismb00-010.php", //
    bibkey = "DBLP:conf/ismb/ChengC00")
public class ChengAndChurch<V extends NumberVector> extends AbstractBiclustering<V, BiclusterWithInversionsModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ChengAndChurch.class);

  /**
   * The minimum number of columns that the database must have so that a removal
   * of columns is performed in {@link #multipleNodeDeletion}.
   * <p>
   * Just start deleting multiple columns when more than 100 columns are in the
   * data matrix.
   */
  private static final int MIN_COLUMN_REMOVE_THRESHOLD = 100;

  /**
   * The minimum number of rows that the database must have so that a removal of
   * rows is performed in {@link #multipleNodeDeletion}.
   * <p>
   * Just start deleting multiple rows when more than 100 rows are in the data
   * matrix.
   * <!--
   * <p>
   * The value is set to 100 as this is not really described in the paper.
   * -->
   */
  private static final int MIN_ROW_REMOVE_THRESHOLD = 100;

  /**
   * Threshold for the score.
   */
  private double delta;

  /**
   * The parameter for multiple node deletion.
   * <p>
   * It is used to magnify the {@link #delta} value in the
   * {@link #multipleNodeDeletion} method.
   */
  private double alpha;

  /**
   * Number of biclusters to be found.
   */
  private int n;

  /**
   * Allow inversion of rows in the last phase.
   */
  private boolean useinverted = true;

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
   * Visitor pattern for processing cells.
   *
   * @author Erich Schubert
   */
  protected interface CellVisitor {
    /** Different modes of operation. */
    int ALL = 0, SELECTED = 1, NOT_SELECTED = 2;

    /**
     * Visit a cell.
     *
     * @param val Value
     * @param row Row Number
     * @param col Column number
     * @param selrow Boolean, whether row is selected
     * @param selcol Boolean, whether column is selected
     * @return Stop flag, return {@code true} to stop visiting
     */
    boolean visit(double val, int row, int col, boolean selrow, boolean selcol);
  }

  /**
   * Bicluster candidate.
   *
   * @author Erich Schubert
   */
  protected static class BiclusterCandidate {
    /**
     * Cardinalities.
     */
    int rowcard, colcard;

    /**
     * Means.
     */
    double[] rowM, colM;

    /**
     * Row and column bitmasks.
     */
    long[] rows, irow, cols;

    /**
     * Mean of the current bicluster.
     */
    double allM;

    /**
     * The current bicluster score (mean squared residue).
     */
    double residue;

    /**
     * Constructor.
     *
     * @param rows Row dimensionality.
     * @param cols Column dimensionality.
     */
    protected BiclusterCandidate(int rows, int cols) {
      super();
      this.rows = BitsUtil.ones(rows);
      this.irow = BitsUtil.zero(rows);
      this.rowcard = rows;
      this.rowM = new double[rows];
      this.cols = BitsUtil.ones(cols);
      this.colcard = cols;
      this.colM = new double[cols];
    }

    /**
     * Resets the values for the next cluster search.
     */
    protected void reset() {
      rows = BitsUtil.ones(rowM.length);
      rowcard = rowM.length;
      cols = BitsUtil.ones(colM.length);
      colcard = colM.length;
      BitsUtil.zeroI(irow);
    }

    /**
     * Visit all selected cells in the data matrix.
     *
     * @param mat Data matrix
     * @param mode Operation mode
     * @param visitor Visitor function
     */
    protected void visitAll(double[][] mat, int mode, CellVisitor visitor) {
      // For efficiency, we manually iterate over the rows and column bitmasks.
      // This saves repeated shifting needed by the manual bit access.
      for(int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
        long rlong = rows[rlpos];
        // Fast skip blocks of 64 masked values.
        if((mode == CellVisitor.SELECTED && rlong == 0L) || (mode == CellVisitor.NOT_SELECTED && rlong == -1L)) {
          rpos += Long.SIZE;
          continue;
        }
        for(int i = 0; i < Long.SIZE && rpos < rowM.length; ++i, ++rpos, rlong >>>= 1) {
          boolean rselected = ((rlong & 1L) == 1L);
          if((mode == CellVisitor.SELECTED && !rselected) || (mode == CellVisitor.NOT_SELECTED && rselected)) {
            continue;
          }
          for(int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
            long clong = cols[clpos];
            if((mode == CellVisitor.SELECTED && clong == 0L) || (mode == CellVisitor.NOT_SELECTED && clong == -1L)) {
              cpos += Long.SIZE;
              continue;
            }
            for(int j = 0; j < Long.SIZE && cpos < colM.length; ++j, ++cpos, clong >>>= 1) {
              boolean cselected = ((clong & 1L) == 1L);
              if((mode == CellVisitor.SELECTED && !cselected) || (mode == CellVisitor.NOT_SELECTED && cselected)) {
                continue;
              }
              boolean stop = visitor.visit(mat[rpos][cpos], rpos, cpos, rselected, cselected);
              if(stop) {
                return;
              }
            }
          }
        }
      }
    }

    /**
     * Visit a column of the matrix.
     *
     * @param mat Data matrix
     * @param col Column to visit
     * @param mode Operation mode
     * @param visitor Visitor function
     */
    protected void visitColumn(double[][] mat, int col, int mode, CellVisitor visitor) {
      boolean cselected = BitsUtil.get(cols, col);
      // For efficiency, we manually iterate over the rows and column bitmasks.
      // This saves repeated shifting needed by the manual bit access.
      for(int rpos = 0, rlpos = 0; rlpos < rows.length; ++rlpos) {
        long rlong = rows[rlpos];
        // Fast skip blocks of 64 masked values.
        if(mode == CellVisitor.SELECTED && rlong == 0L) {
          rpos += Long.SIZE;
          continue;
        }
        if(mode == CellVisitor.NOT_SELECTED && rlong == -1L) {
          rpos += Long.SIZE;
          continue;
        }
        for(int i = 0; i < Long.SIZE && rpos < rowM.length; ++i, ++rpos, rlong >>>= 1) {
          boolean rselected = ((rlong & 1L) == 1L);
          if(mode == CellVisitor.SELECTED && !rselected) {
            continue;
          }
          if(mode == CellVisitor.NOT_SELECTED && rselected) {
            continue;
          }
          boolean stop = visitor.visit(mat[rpos][col], rpos, col, rselected, cselected);
          if(stop) {
            return;
          }
        }
      }
    }

    /**
     * Visit a row of the data matrix.
     *
     * @param mat Data matrix
     * @param row Row to visit
     * @param visitor Visitor function
     */
    protected void visitRow(double[][] mat, int row, int mode, CellVisitor visitor) {
      boolean rselected = BitsUtil.get(rows, row);
      final double[] rowdata = mat[row];
      for(int cpos = 0, clpos = 0; clpos < cols.length; ++clpos) {
        long clong = cols[clpos];
        // Fast skip blocks of 64 masked values.
        if(mode == CellVisitor.SELECTED && clong == 0L) {
          cpos += Long.SIZE;
          continue;
        }
        if(mode == CellVisitor.NOT_SELECTED && clong == -1L) {
          cpos += Long.SIZE;
          continue;
        }
        for(int j = 0; j < Long.SIZE && cpos < colM.length; ++j, ++cpos, clong >>>= 1) {
          boolean cselected = ((clong & 1L) == 1L);
          if(mode == CellVisitor.SELECTED && !cselected) {
            continue;
          }
          if(mode == CellVisitor.NOT_SELECTED && cselected) {
            continue;
          }
          boolean stop = visitor.visit(rowdata[cpos], row, cpos, rselected, cselected);
          if(stop) {
            return;
          }
        }
      }
    }

    /** Visitor for updating the means. */
    private final CellVisitor MEANVISITOR = new CellVisitor() {
      @Override
      public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
        if(selcol) {
          rowM[row] += val;
        }
        if(selrow) {
          colM[col] += val;
        }
        if(selcol && selrow) {
          allM += val;
        }
        return false;
      }
    };

    /**
     * Update the row means and column means.
     *
     * @param mat Data matrix
     * @param all Flag, to update all
     * @return overall mean
     */
    protected double updateRowAndColumnMeans(final double[][] mat, boolean all) {
      final int mode = all ? CellVisitor.ALL : CellVisitor.SELECTED;
      Arrays.fill(rowM, 0.);
      Arrays.fill(colM, 0.);
      allM = 0.;
      visitAll(mat, mode, MEANVISITOR);
      visitColumn(mat, 0, mode, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          rowM[row] /= colcard;
          return false;
        }
      });
      visitRow(mat, 0, mode, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          colM[col] /= rowcard;
          return false;
        }
      });
      allM /= colcard * rowcard;
      return allM;
    }

    /**
     * Compute the mean square residue.
     *
     * @param mat Data matrix
     * @return mean squared residue
     */
    protected double computeMeanSquaredDeviation(final double[][] mat) {
      final Mean msr = new Mean();
      visitAll(mat, CellVisitor.SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (selrow && selcol);
          double v = val - rowM[row] - colM[col] + allM;
          msr.put(v * v);
          return false;
        }
      });
      residue = msr.getMean();
      return residue;
    }

    /**
     * Computes the <b>mean row residue</b> of the given <code>row</code>.
     *
     * @param mat Data matrix
     * @param row The row who's residue should be computed.
     * @param rowinverted Indicates if the row should be considered inverted.
     * @return The row residue of the given <code>row</code>.
     */
    protected double computeRowResidue(final double[][] mat, int row, final boolean rowinverted) {
      final Mean rowResidue = new Mean();
      visitRow(mat, row, CellVisitor.SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (selcol);
          final double rowMean = rowM[row];
          final double colMean = colM[col];
          double v = ((!rowinverted) ? (val - rowMean) : (rowMean - val)) - colMean + allM;
          rowResidue.put(v * v);
          return false;
        }
      });
      return rowResidue.getMean();
    }

    /**
     *
     * Computes the <b>mean column residue</b> of the given <code>col</code>.
     *
     * @param col The column who's residue should be computed.
     * @return The row residue of the given <code>col</code>um.
     */
    protected double computeColResidue(final double[][] mat, final int col) {
      final double bias = colM[col] - allM;
      final Mean colResidue = new Mean();
      visitColumn(mat, col, CellVisitor.SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (selrow);
          final double rowMean = rowM[row];
          double v = val - rowMean - bias;
          colResidue.put(v * v);
          return false;
        }
      });
      return colResidue.getMean();
    }

    /**
     * Updates the mask with replacement values for all data in the given rows
     * and columns.
     *
     * @param mat Mask to update.
     * @param replacement Distribution to sample replacement values from.
     */
    protected void maskMatrix(final double[][] mat, final Distribution replacement) {
      visitAll(mat, CellVisitor.SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (selrow && selcol);
          mat[row][col] = replacement.nextRandom();
          return false;
        }
      });
    }

    /**
     * Select or deselect a column.
     *
     * @param cnum Column to select
     * @param set Value to set
     */
    protected void selectColumn(int cnum, boolean set) {
      if(set) {
        BitsUtil.setI(cols, cnum);
        colcard++;
      }
      else {
        BitsUtil.clearI(cols, cnum);
        colcard--;
      }
    }

    /**
     * Select or deselect a row.
     *
     * @param rnum Row to select
     * @param set Value to set
     */
    protected void selectRow(int rnum, boolean set) {
      if(set) {
        BitsUtil.setI(rows, rnum);
        rowcard++;
      }
      else {
        BitsUtil.clearI(rows, rnum);
        rowcard--;
      }
    }

    protected void invertRow(int rnum, boolean b) {
      BitsUtil.setI(irow, rnum);
    }
  }

  @Override
  public Clustering<BiclusterWithInversionsModel> biclustering() {
    double[][] mat = RelationUtil.relationAsMatrix(relation, rowIDs);

    BiclusterCandidate cand = new BiclusterCandidate(getRowDim(), getColDim());

    Clustering<BiclusterWithInversionsModel> result = new Clustering<>("Cheng-and-Church", "Cheng and Church Biclustering");
    ModifiableDBIDs noise = DBIDUtil.newHashSet(relation.getDBIDs());

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Extracting Cluster", n, LOG) : null;
    for(int i = 0; i < n; i++) {
      cand.reset();
      multipleNodeDeletion(mat, cand);
      if(LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 2: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
      }
      singleNodeDeletion(mat, cand);
      if(LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 1: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
      }
      nodeAddition(mat, cand);
      if(LOG.isVeryVerbose()) {
        LOG.veryverbose("Residue after Alg 3: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
      }
      cand.maskMatrix(mat, dist);
      BiclusterWithInversionsModel model = new BiclusterWithInversionsModel(colsBitsetToIDs(cand.cols), rowsBitsetToIDs(cand.irow));
      final ArrayDBIDs cids = rowsBitsetToIDs(cand.rows);
      noise.removeDBIDs(cids);
      result.addToplevelCluster(new Cluster<>(cids, model));

      if(LOG.isVerbose()) {
        LOG.verbose("Score of bicluster " + (i + 1) + ": " + cand.residue + "\n");
        LOG.verbose("Number of rows: " + cand.rowcard + "\n");
        LOG.verbose("Number of columns: " + cand.colcard + "\n");
        // LOG.verbose("Total number of masked values: " + maskedVals.size() +
        // "\n");
      }
      LOG.incrementProcessed(prog);
    }
    // Add a noise cluster, full-dimensional.
    if(!noise.isEmpty()) {
      long[] allcols = BitsUtil.ones(getColDim());
      BiclusterWithInversionsModel model = new BiclusterWithInversionsModel(colsBitsetToIDs(allcols), DBIDUtil.EMPTYDBIDS);
      result.addToplevelCluster(new Cluster<>(noise, true, model));
    }
    LOG.ensureCompleted(prog);
    return result;
  }

  /**
   * Algorithm 1 of Cheng and Church:
   * <p>
   * Remove single rows or columns.
   * <p>
   * Inverted rows are not supported in this method.
   *
   * @param mat Data matrix
   * @param cand Bicluster candidate
   */
  private void singleNodeDeletion(final double[][] mat, final BiclusterCandidate cand) {
    // Assume that cand.residue is up to date!
    while(cand.residue > delta && (cand.colcard > 2 || cand.rowcard > 2)) {
      // Store current maximum. Need final mutable, so use arrays.
      final double[] max = { Double.NEGATIVE_INFINITY };
      final int[] best = { -1, -1 };

      // Test rows
      if(cand.rowcard > 2) {
        cand.visitColumn(mat, 0, CellVisitor.SELECTED, new CellVisitor() {
          @Override
          public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
            assert (selrow);
            double rowResidue = cand.computeRowResidue(mat, row, false);
            if(max[0] < rowResidue) {
              max[0] = rowResidue;
              best[0] = row;
            }
            return false;
          }
        });
      }

      // Test columns:
      if(cand.colcard > 2) {
        cand.visitRow(mat, 0, CellVisitor.SELECTED, new CellVisitor() {
          @Override
          public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
            assert (selcol);
            double colResidue = cand.computeColResidue(mat, col);
            if(max[0] < colResidue) {
              max[0] = colResidue;
              best[1] = col;
            }
            return false;
          }
        });
      }

      if(best[1] >= 0) { // then override bestrow!
        cand.selectColumn(best[1], false);
      }
      else {
        assert (best[0] >= 0);
        cand.selectRow(best[0], false);
      }
      // TODO: incremental update could be much faster?
      cand.updateRowAndColumnMeans(mat, false);
      cand.computeMeanSquaredDeviation(mat);
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Residue in Alg 1: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
      }
    }
  }

  //
  /**
   * Algorithm 2 of Cheng and Church.
   * <p>
   * Remove all rows and columns that reduce the residue by alpha.
   * <p>
   * Inverted rows are not supported in this method.
   *
   * @param mat Data matrix
   * @param cand Bicluster candidate
   */
  private void multipleNodeDeletion(final double[][] mat, final BiclusterCandidate cand) {
    cand.updateRowAndColumnMeans(mat, false);
    cand.computeMeanSquaredDeviation(mat);

    // Note: assumes that cand.residue = H(I,J)
    while(cand.residue > delta) {
      final boolean[] modified = { false, false };

      // Step 2: remove rows above threshold
      if(cand.rowcard > MIN_ROW_REMOVE_THRESHOLD) {
        final double alphaResidue = alpha * cand.residue;
        cand.visitColumn(mat, 0, CellVisitor.SELECTED, new CellVisitor() {
          @Override
          public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
            assert (selrow);
            if(cand.computeRowResidue(mat, row, false) > alphaResidue) {
              cand.selectRow(row, false);
              modified[0] = true;
            }
            return (cand.rowcard > MIN_ROW_REMOVE_THRESHOLD);
          }
        });

        // Step 3: update residue
        if(modified[0]) {
          cand.updateRowAndColumnMeans(mat, false);
          cand.computeMeanSquaredDeviation(mat);
        }
      }

      // Step 4: remove columns above threshold
      if(cand.colcard > MIN_COLUMN_REMOVE_THRESHOLD) {
        final double alphaResidue = alpha * cand.residue;
        cand.visitRow(mat, 0, CellVisitor.SELECTED, new CellVisitor() {
          @Override
          public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
            assert (selcol);
            if(cand.computeColResidue(mat, col) > alphaResidue) {
              cand.selectColumn(col, false);
              modified[1] = true;
            }
            return (cand.colcard > MIN_COLUMN_REMOVE_THRESHOLD);
          }
        });
        if(modified[1]) {
          cand.updateRowAndColumnMeans(mat, false);
          cand.computeMeanSquaredDeviation(mat);
        }
      }

      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Residue in Alg 2: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
      }
      // Step 5: if nothing has been removed, try removing single nodes.
      if(!modified[0] && !modified[1]) {
        break;
        // Will be executed next in main loop, as per algorithm 4.
        // singleNodeDeletion();
      }
    }
  }

  /**
   * Algorithm 3 of Cheng and Church.
   * <p>
   * Try to re-add rows or columns that decrease the overall score.
   * <p>
   * Also try adding inverted rows.
   *
   * @param mat Data matrix
   * @param cand Bicluster candidate
   */
  private void nodeAddition(final double[][] mat, final BiclusterCandidate cand) {
    cand.updateRowAndColumnMeans(mat, true);
    cand.computeMeanSquaredDeviation(mat);
    while(true) {
      // We need this to be final + mutable
      final boolean[] added = new boolean[] { false, false };

      // Step 2: add columns
      cand.visitRow(mat, 0, CellVisitor.NOT_SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (!selcol);
          if(cand.computeColResidue(mat, col) <= cand.residue) {
            cand.selectColumn(col, true);
            added[0] = true;
          }
          return false;
        }
      });

      // Step 3: recompute values
      if(added[0]) {
        cand.updateRowAndColumnMeans(mat, true);
        cand.computeMeanSquaredDeviation(mat);
      }

      // Step 4: try adding rows.
      cand.visitColumn(mat, 0, CellVisitor.NOT_SELECTED, new CellVisitor() {
        @Override
        public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
          assert (!selrow);
          if(cand.computeRowResidue(mat, row, false) <= cand.residue) {
            cand.selectRow(row, true);
            added[1] = true;
          }
          return false;
        }
      });

      // Step 5: try adding inverted rows.
      if(useinverted) {
        cand.visitColumn(mat, 0, CellVisitor.NOT_SELECTED, new CellVisitor() {
          @Override
          public boolean visit(double val, int row, int col, boolean selrow, boolean selcol) {
            assert (!selrow);
            if(cand.computeRowResidue(mat, row, true) <= cand.residue) {
              cand.selectRow(row, true);
              cand.invertRow(row, true);
              added[1] = true;
            }
            return false;
          }
        });
      }
      if(added[1]) {
        cand.updateRowAndColumnMeans(mat, true);
        cand.computeMeanSquaredDeviation(mat);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Residue in Alg 3: " + cand.residue + " " + cand.rowcard + "x" + cand.colcard);
        }
      }
      if(!added[0] && !added[1]) {
        break;
      }
    }
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
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the distribution of replacement values when masking
     * a cluster.
     */
    public static final OptionID DIST_ID = new OptionID("chengandchurch.replacement", "Distribution of replacement values when masking found clusters.");

    /**
     * Threshold value to determine the maximal acceptable score (mean squared
     * residue) of a bicluster.
     */
    public static final OptionID DELTA_ID = new OptionID("chengandchurch.delta", "Threshold value to determine the maximal acceptable score (mean squared residue) of a bicluster.");

    /**
     * Parameter for multiple node deletion to accelerate the algorithm.
     */
    public static final OptionID ALPHA_ID = new OptionID("chengandchurch.alpha", "Parameter for multiple node deletion to accelerate the algorithm.");

    /**
     * Number of biclusters to be found.
     */
    public static final OptionID N_ID = new OptionID("chengandchurch.n", "The number of biclusters to be found.");

    /**
     * Threshold for the score ({@link #DELTA_ID}).
     */
    private double delta;

    /**
     * The parameter for multiple node deletion.
     * <p>
     * It is used to magnify the {@link #delta} value in the
     * {@link ChengAndChurch#multipleNodeDeletion} method.
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
      DoubleParameter deltaP = new DoubleParameter(DELTA_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }

      IntParameter nP = new IntParameter(N_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(nP)) {
        n = nP.intValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 1.) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      ObjectParameter<Distribution> distP = new ObjectParameter<>(DIST_ID, Distribution.class, UniformDistribution.class);
      if(config.grab(distP)) {
        dist = distP.instantiateClass(config);
      }
    }

    @Override
    protected ChengAndChurch<V> makeInstance() {
      return new ChengAndChurch<>(delta, alpha, n, dist);
    }
  }
}
