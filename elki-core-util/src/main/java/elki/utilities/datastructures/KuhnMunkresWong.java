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

import elki.utilities.documentation.Reference;

/**
 * Kuhn-Munkres optimal matching (aka the Hungarian algorithm), supposedly in a
 * modern variant. This is a popular algorithm to find the best 1:1 matching
 * to minimize the cost. The original version has a worst case of
 * O(n<sup>4</sup>). By caching minima and storing column and row deltas,
 * as discussed by Wong, the implementation is supposedly able to guarantee
 * O(n³) time complexity. In our experiments, it was substantially faster for
 * large matrixes, computing a 1000x1000 in less than a second, making this
 * often acceptable to run on data sets with a few thousand objects.
 * <p>
 * References:
 * <p>
 * J. K. Wong<br>
 * A new implementation of an algorithm for the optimal assignment problem:
 * An improved version of Munkres' algorithm<br>
 * BIT Numerical Mathematics 19(3)
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "J. K. Wong", //
    title = "A new implementation of an algorithm for the optimal assignment problem: An improved version of Munkres' algorithm", //
    booktitle = "BIT Numerical Mathematics 19(3)", //
    url = "https://doi.org/10.1007/BF01930994", //
    bibkey = "doi:10.1007/BF01930994")
public class KuhnMunkresWong extends KuhnMunkres {
  /**
   * Row adjustment
   */
  double[] radj;

  /**
   * Column bias offsets
   */
  double[] cadj;

  /**
   * Minimum value of non-marked columns, for each row.
   */
  double[] rmin;

  /**
   * Pointer to the minimum non-marked value, for each row.
   */
  int[] rptr;

  /**
   * Perform the Kuhn-Munkres algorithm.
   *
   * @param cost Cost matrix
   * @return Selected columns, per row
   */
  @Override
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
    this.rmin = new double[rowlen];
    this.rptr = new int[rowlen];
    // Iterative refinement:
    for(long maxit = rowlen * (long) collen; maxit >= 0 && selected < rowlen; maxit--) {
      // O(n³) total, because this gets executed at most n times?
      initUncoveredMinimum(); // O(n²)
      while(true) {
        double h = findUncoveredMinimum(); // improved version O(n)
        debugLogMatrix(Level.FINEST, maxit, "Select min");
        removeCost(h); // if >0, improved version O(n)
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

  @Override
  protected void initialize(double[][] cost) {
    this.cost = cost;
    final int rowlen = cost.length, collen = cost[0].length;
    double[] radj = this.radj = new double[rowlen];
    for(int i = 0; i < cost.length; i++) {
      double[] row = cost[i];
      if(row.length != collen) {
        throw new IllegalStateException("Cost matrix is not rectangular.");
      }
      // Find row minima:
      double m = row[0];
      for(int j = 1; j < row.length; j++) {
        final double v = row[j];
        m = m <= v ? m : v;
      }
      radj[i] = m;
    }
    // Use column minima only for square?
    if(rowlen == collen) {
      double[] cadj = this.cadj = cost[0].clone();
      double radj0 = radj[0];
      for(int j = 0; j < cadj.length; j++) {
        cadj[j] -= radj0;
      }
      for(int i = 1; i < cost.length; i++) {
        final double[] rowi = cost[i];
        double radji = radj[i];
        for(int j = 0; j < radj.length; j++) {
          final double v = rowi[j] - radji, mj = cadj[j];
          if(v < mj) {
            cadj[j] = v;
          }
        }
      }
    }
    else {
      this.cadj = new double[collen];
    }
  }

  @Override
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
      final double radji = radj[i];
      for(int j = csel.length - 1; j >= 0; j--) {
        if(csel[j] < 0 && costi[j] - radji - cadj[j] == 0) {
          rsel[i] = j;
          csel[j] = i;
          ++selected;
          break;
        }
      }
    }
  }

  /**
   * Initialize values for finding the minima efficiently.
   */
  protected void initUncoveredMinimum() {
    Arrays.fill(rmark, -1);
    for(int i = 0; i < cost.length; i++) {
      final double[] rowi = cost[i];
      final double radji = radj[i];
      double m = Double.POSITIVE_INFINITY;
      int b = -1;
      for(int j = 0; j < rowi.length; j++) {
        if(cmark[j] < 0) {
          final double v = rowi[j] - radji - cadj[j];
          if(v < m) {
            m = v;
            b = j;
          }
        }
      }
      rmin[i] = m;
      rptr[i] = b;
    }
  }

  /**
   * Find the minimum in the uncovered rows.
   *
   * @return Minimum in uncovered rows
   */
  protected double findUncoveredMinimum() {
    minr = minc = -1;
    double h = Double.POSITIVE_INFINITY;
    for(int i = 0; i < rmin.length; i++) {
      if(rmark[i] < 0) {
        double m = rmin[i];
        if(m < h) {
          h = m;
          minr = i;
          minc = rptr[i];
        }
      }
    }
    assert minr >= 0 && rsel[minr] != minc;
    return h;
  }

  /**
   * Remove cost h (if &gt; 0) found to be unavoidable.
   *
   * @param h Cost to remove
   */
  protected void removeCost(double h) {
    if(h > 0) {
      for(int i = 0; i < rmark.length; i++) {
        if(rmark[i] >= 0) {
          radj[i] -= h;
        }
        else {
          rmin[i] -= h;
        }
      }
      for(int j = 0; j < cmark.length; j++) {
        final int r = cmark[j];
        if(r < 0 || rmark[r] == minr) {
          cadj[j] += h;
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
        // Update rmin, because we removed cmark on j:
        final double cadjj = cadj[j];
        for(int i = 0; i < rmin.length; i++) {
          final double c = cost[i][j] - radj[i] - cadjj;
          if(c < rmin[i]) {
            rmin[i] = c;
            rptr[i] = j;
          }
        }
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

  // For debug output
  @Override
  protected double costOf(int i, int j) {
    return cost[i][j] - radj[i] - cadj[j];
  }
}
