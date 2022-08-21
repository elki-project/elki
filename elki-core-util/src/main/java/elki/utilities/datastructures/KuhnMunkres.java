/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.utilities.datastructures;

import java.util.Arrays;
import java.util.logging.Level;

import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.io.FormatUtil;

/**
 * Kuhn-Munkres optimal matching (aka the Hungarian algorithm). This is a
 * popular algorithm to find the best 1:1 matching to minimize the cost.
 * The original version has a worst case of O(n<sup>4</sup>) albeit sometimes
 * given as O(n³). Empirically, this version is O(n³), too, but literature seems
 * to disagree. For an improved O(n³) version, see {@link KuhnMunkresWong}.
 * <p>
 * As a rule of thumb, a 1000x1000 problem may take around 30-60 seconds
 * depending on your CPU, a 10kx10k problem may hence take 8-16 hours already.
 * <p>
 * References:
 * <p>
 * H. W. Kuhn<br>
 * The Hungarian method for the assignment problem<br>
 * Naval Research Logistics Quarterly 2
 * <p>
 * J. Munkres<br>
 * Algorithms for the Assignment and Transportation Problems<br>
 * Journal of the Society for Industrial and Applied Mathematics 5(1)
 * <p>
 * TODO: also implement the Jonker-Volgenant variant!
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "H. W. Kuhn", //
    title = "The Hungarian method for the assignment problem", //
    booktitle = "Naval Research Logistics Quarterly 2", //
    url = "https://doi.org/10.1002/nav.3800020109", //
    bibkey = "doi:10.1002/nav.3800020109")
@Reference(authors = "J. Munkres", //
    title = "Algorithms for the Assignment and Transportation Problems", //
    booktitle = "Journal of the Society for Industrial and Applied Mathematics 5(1)", //
    url = "https://doi.org/10.1137/0105003", //
    bibkey = "doi:10.1137/0105003")
public class KuhnMunkres {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(KuhnMunkres.class);

  /**
   * Cost matrix
   */
  double[][] cost;

  /**
   * Number of selected zeros.
   */
  int selected;

  /**
   * Selected ("starred") column per row, -1 if not selected.
   */
  int[] rsel;

  /**
   * Selected ("starred") row per column, -1 if not selected.
   */
  int[] csel;

  /**
   * Row marks, used only temporary.
   */
  int[] rmark;

  /**
   * Column marks.
   */
  int[] cmark;

  /**
   * Position of uncovered minimum row
   */
  int minr = -1;

  /**
   * Position of uncovered minimum column
   */
  int minc = -1;

  /**
   * Perform the Kuhn-Munkres algorithm.
   *
   * @param cost Cost matrix
   * @return Selected columns, per row
   */
  public int[] run(double[][] cost) {
    final int rowlen = cost.length, collen = cost[0].length;
    if(collen < rowlen) {
      throw new IllegalStateException("Cost matrix must not have fewer columns than rows.");
    }
    // Find initial cost offsets, using row- and column minima:
    initialize(cost); // O(n²)
    // Perform an initial greedy selection, mark columns
    initialCover(); // O(n²)
    if(selected == rowlen) {
      debugLogMatrix(Level.FINEST, -1, "trivial solution");
      return rsel;
    }
    this.rmark = new int[rowlen];
    this.cmark = csel.clone();
    // Iterative refinement:
    for(long maxit = rowlen * (long) collen; maxit >= 0 && selected < rowlen; maxit--) {
      Arrays.fill(rmark, -1);
      while(true) {
        double h = findUncoveredMinimum(); // O(n²)
        debugLogMatrix(Level.FINEST, maxit, "Select min");
        removeCost(h); // if >0, (n²)
        // Pivot on zero at ij.
        if(!pivot()) {
          debugLogMatrix(Level.FINEST, maxit, "Update stars");
          updateStars();
          System.arraycopy(csel, 0, cmark, 0, csel.length);
          break;
        }
        debugLogMatrix(Level.FINEST, maxit, "Pivoted");
      }
    }
    debugLogMatrix(Level.FINEST, 0, "end " + selected);
    assert selected == rowlen;
    return rsel;
  }

  /**
   * Initialize, and make a deep copy.
   * 
   * @param ocost Original cost matrix
   */
  protected void initialize(double[][] ocost) {
    double[][] cost = this.cost = new double[ocost.length][];
    final int rowlen = ocost.length, collen = ocost[0].length;
    for(int i = 0; i < rowlen; i++) {
      double[] row = cost[i] = ocost[i].clone(); // Deep copy!
      if(row.length != collen) {
        throw new IllegalStateException("Cost matrix is not rectangular.");
      }
      // Find row minima:
      double m = row[0];
      for(int j = 1; j < row.length; j++) {
        final double v = row[j];
        assert !Double.isNaN(v);
        m = m <= v ? m : v;
      }
      for(int j = 0; j < row.length; j++) {
        row[j] -= m;
      }
    }
    // Use column minima only for square?
    if(rowlen == collen) {
      double[] m = cost[0].clone();
      for(int i = 1; i < rowlen; i++) {
        final double[] rowi = cost[i];
        for(int j = 0; j < collen; j++) {
          final double vj = rowi[j], mj = m[j];
          if(vj < mj) {
            m[j] = vj;
          }
        }
      }
      for(int i = 0; i < rowlen; i++) {
        double[] rowi = cost[i];
        for(int j = 0; j < collen; j++) {
          rowi[j] -= m[j];
        }
      }
    }
  }

  /**
   * Select the last zero in each row to make an initial selection, which may
   * already yield a solution.
   */
  protected void initialCover() {
    final double[][] cost = this.cost;
    final int rowlen = cost.length, collen = cost[0].length;
    int[] rsel = this.rsel = new int[rowlen];
    int[] csel = this.csel = new int[collen];
    Arrays.fill(rsel, -1);
    Arrays.fill(csel, -1);
    selected = 0;
    // Jonker and Volgenant suggest backwards order
    for(int i = rsel.length - 1; i >= 0; i--) {
      final double[] costi = cost[i];
      for(int j = csel.length - 1; j >= 0; j--) {
        if(csel[j] < 0 && costi[j] == 0) {
          rsel[i] = j;
          csel[j] = i;
          ++selected;
          break;
        }
      }
    }
  }

  /**
   * Find the minimum in the uncovered rows.
   *
   * @return Minimum in uncovered rows
   */
  private double findUncoveredMinimum() {
    minr = minc = -1;
    double h = Double.POSITIVE_INFINITY;
    for(int i = 0; i < cost.length; i++) {
      if(rmark[i] < 0) {
        double[] rowi = cost[i];
        for(int j = 0; j < rowi.length; j++) {
          if(cmark[j] < 0 || rmark[cmark[j]] == i) {
            final double v = rowi[j];
            if(v < h) {
              h = v;
              minr = i;
              minc = j;
            }
          }
        }
      }
    }
    assert minr >= 0;
    return h;
  }

  /**
   * Remove cost h (if &gt; 0) found to be unavoidable.
   *
   * @param h Cost to remove
   */
  private void removeCost(double h) {
    if(h > 0) {
      for(int i = 0; i < rmark.length; i++) {
        double[] rowi = cost[i];
        if(rmark[i] >= 0) {
          for(int j = 0; j < rowi.length; j++) {
            if(cmark[j] >= 0 && rmark[cmark[j]] != minr) {
              rowi[j] += h;
            }
          }
        }
        else {
          for(int j = 0; j < cmark.length; j++) {
            if(cmark[j] < 0 || rmark[cmark[j]] == minr) {
              rowi[j] -= h;
            }
          }
        }
      }
    }
  }

  /**
   * Pivot columns mark to row marks.
   * 
   * @return {@code true} if we pivoted.
   */
  private boolean pivot() {
    assert rmark[minr] < 0;
    for(int j = 0; j < cmark.length; j++) {
      if(cmark[j] == minr) {
        assert rsel[minr] >= 0;
        cmark[j] = -1; // Unmark column.
        rmark[minr] = minc; // Mark row.
        return true;
      }
    }
    return false;
  }

  /**
   * Update the position of stars.
   */
  private void updateStars() {
    while(minc >= 0) {
      int roldstar = csel[minc];
      assert roldstar != minr;
      assert rsel[minr] == -1; // csel may be > -1!
      rsel[minr] = minc;
      csel[minc] = cmark[minc] = minr;
      ++selected;
      if(roldstar < 0) {
        break;
      }
      rsel[roldstar] = -1;
      // csel was already overwritten!
      --selected;
      minc = rmark[minr = roldstar];
    }
  }

  /**
   * Debug logging of the matrix.
   * 
   * @param l Log level
   * @param maxit Iteration countdown
   * @param msg Message prefix
   */
  protected void debugLogMatrix(Level l, long maxit, String msg) {
    if(LOG.isLoggable(l)) {
      String padding = "      ";
      StringBuilder buf = new StringBuilder(cost.length * csel.length * 10 + 10) //
          .append('#').append(1L + cost.length * (long) csel.length - maxit) //
          .append(' ').append(msg).append("\n");
      for(int i = 0; i < cost.length; i++) {
        for(int j = 0; j < csel.length; j++) {
          final int pos = buf.length();
          if(rsel[i] == j) {
            buf.append('*');
          }
          else if(cmark != null && cmark[j] == i) {
            buf.append('|');
          }
          // TODO: choose NF depending on value range.
          buf.append(FormatUtil.NF2.format(costOf(i, j)));
          int p = 7 - (buf.length() - pos);
          if(p > 0) {
            buf.insert(pos, padding, 0, p);
          }
          if(rmark != null && rmark[i] == j) {
            buf.append('\'');
          }
          else if(minr == i && minc == j) {
            buf.append('!');
          }
          p = 8 - (buf.length() - pos);
          if(p > 0) {
            buf.append(padding, 0, p);
          }
          buf.append(' ');
        }
        buf.append(rmark != null && rmark[i] >= 0 ? "--\n" : "\n");
      }
      if(cmark != null) {
        for(int j = 0; j < cmark.length; j++) {
          buf.append(cmark[j] < 0 ? "         " : "    |    ");
        }
      }
      LOG.log(l, buf.toString());
    }
  }

  /**
   * Get the adjusted cost of a single element, for debug output.
   *
   * @param i row
   * @param j column
   * @return cost
   */
  protected double costOf(int i, int j) {
    return cost[i][j];
  }
}
