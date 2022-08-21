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
 * A version of Kuhn-Munkres inspired by the implementation of Kevin L. Stern.
 * This approach shares some ideas of {@link KuhnMunkresWong}, but works on
 * columns instead of rows first. So far, we have not found a good reference why
 * this works much faster is practice; but it does.
 * <p>
 * References:
 * <p>
 * K. L. Stern<br>
 * The Hungarian Algorithm for the Assignment Problem<br>
 * http://software-and-algorithms.blogspot.com/2012/09/the-hungarian-algorithm-for-assignment.html
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "K. L. Stern", //
    title = "The Hungarian Algorithm for the Assignment Problem", //
    booktitle = "Online", //
    url = "http://software-and-algorithms.blogspot.com/2012/09/the-hungarian-algorithm-for-assignment.html", //
    bibkey = "web/Stern12")
public class KuhnMunkresStern extends KuhnMunkresWong {
  /**
   * Pointer to the minimum non-marked value, for each column;
   * but initially to the starting row.
   */
  private int[] cptr;

  /**
   * Minimum value of non-marked row, for each column.
   */
  private double[] cmin;

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
    this.cmin = new double[rowlen];
    this.cptr = new int[rowlen];

    // Iterative refinement:
    for(long maxit = rowlen * (long) collen; maxit >= 0 && selected < rowlen; maxit--) {
      initUncoveredMinimum();
      while(true) {
        double h = findUncoveredMinimum();
        removeCost(h);
        cmark[minc] = minr;
        if(csel[minc] < 0) {
          // Augmenting path
          int j = minc;
          while(j >= 0) {
            final int i = cmark[j];
            final int nextj = rsel[i];
            rsel[i] = j;
            csel[j] = i;
            j = nextj;
          }
          ++selected;
          break;
        }
        final int i = csel[minc];
        rmark[i] = minc;
        // Update cmin, cptr
        final double[] costi = cost[i];
        final double radji = radj[i];
        for(int j = 0; j < cmark.length; j++) {
          if(cmark[j] < 0) {
            double c = costi[j] - radji - cadj[j];
            if(c < cmin[j]) {
              cmin[j] = c;
              cptr[j] = i;
            }
          }
        }
      }
    }
    return rsel;
  }

  @Override
  protected void initUncoveredMinimum() {
    int i = -1;
    while(++i < rsel.length) {
      if(rsel[i] == -1) {
        break;
      }
    }
    if(i == rsel.length) {
      return;
    }
    Arrays.fill(rmark, -1);
    Arrays.fill(cmark, -1);
    rmark[i] = Integer.MAX_VALUE;
    for(int j = 0; j < cost[i].length; j++) {
      cmin[j] = cost[i][j] - radj[i] - cadj[j];
      cptr[j] = i;
    }
    return;
  }

  @Override
  protected double findUncoveredMinimum() {
    minr = minc = -1;
    double h = Double.POSITIVE_INFINITY;
    for(int j = 0; j < cmark.length; j++) {
      if(cmark[j] < 0) {
        final double m = cmin[j];
        if(m < h) {
          h = m;
          minr = cptr[j];
          minc = j;
        }
      }
    }
    return h;
  }

  @Override
  protected void removeCost(double h) {
    if(h > 0) {
      for(int i = 0; i < rmark.length; i++) {
        if(rmark[i] >= 0) {
          radj[i] += h;
        }
      }
      for(int j = 0; j < cmark.length; j++) {
        if(cmark[j] >= 0) {
          cadj[j] -= h;
        }
        else {
          cmin[j] -= h;
        }
      }
    }
  }
}
