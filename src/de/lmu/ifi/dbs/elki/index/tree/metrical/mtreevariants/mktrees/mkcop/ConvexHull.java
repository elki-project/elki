package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

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

/**
 * Holds the lower and upper hull for some values.
 * 
 * @author Elke Achtert
 */
public class ConvexHull {
  /**
   * The lower hull.
   */
  private int[] lowerHull;

  /**
   * The upper hull.
   */
  private int[] upperHull;

  /**
   * Number of points in lower hull.
   */
  private int l;

  /**
   * Number of points in upper hull.
   */
  private int u;

  /**
   * Creates a new convex hull for the specified distances.
   * 
   * @param x the x-values of the points for which the lower and upper hull
   *        should be computed
   * @param y the y-values of the points for which the lower and upper hull
   *        should be computed
   */
  public ConvexHull(double[] x, double[] y) {
    if(x.length != y.length) {
      throw new IllegalArgumentException("x and y have different lengths!");
    }

    lowerHull = new int[x.length];
    upperHull = new int[x.length];
    determineLowerAndUpperHull(x, y);
  }

  /**
   * Returns the lower hull.
   * 
   * @return the lower hull
   */
  public int[] getLowerHull() {
    return lowerHull;
  }

  /**
   * Returns the upper hull.
   * 
   * @return the upper hull
   */
  public int[] getUpperHull() {
    return upperHull;
  }

  /**
   * Returns the number of points in lower hull
   * 
   * @return the number of points in lower hull
   */
  public int getNumberOfPointsInLowerHull() {
    return l;
  }

  /**
   * Returns the number of points in upper hull
   * 
   * @return the number of points in upper hull
   */
  public int getNumberOfPointsInUpperHull() {
    return u;
  }

  /**
   * Computes the lower and upper hull of the specified distances.
   * 
   * @param x the x-values of the points for which the lower and upper hull
   *        should be computed
   * @param y the y-values of the points for which the lower and upper hull
   *        should be computed
   */
  private void determineLowerAndUpperHull(double[] x, double[] y) {
    StringBuffer msg = new StringBuffer();
    // first point is always in lowerHull and upperHull
    lowerHull[0] = 0;
    l = 1;
    upperHull[0] = 0;
    u = 1;

    // Determine the convex hulls (using point stack)
    for(int i = 1; i < x.length; i++) {
      // lower hull
      lowerHull[l] = i;
      while(l >= 2 && (y[lowerHull[l]] - y[lowerHull[l - 1]]) / (x[lowerHull[l]] - x[lowerHull[l - 1]]) <= (y[lowerHull[l - 1]] - y[lowerHull[l - 2]]) / (x[lowerHull[l - 1]] - x[lowerHull[l - 2]])) {
        // right curved
        lowerHull[l - 1] = lowerHull[l];
        this.l--;
      }
      this.l++;

      // upper hull
      upperHull[u] = i;
      while(u >= 2 && (y[upperHull[u]] - y[upperHull[u - 1]]) / (x[upperHull[u]] - x[upperHull[u - 1]]) >= (y[upperHull[u - 1]] - y[upperHull[u - 2]]) / (x[upperHull[u - 1]] - x[upperHull[u - 2]])) {
        // left curved
        upperHull[u - 1] = upperHull[u];
        u--;
      }
      u++;
    }

    msg.append("lower and upper hull\n");
    for(int i = 0; i < i; i++) {
      msg.append("  uhull ").append(i).append("=").append(upperHull[i]).append(" y=").append(y[upperHull[i]]).append("\n");
      msg.append("  lhull ").append(i).append("=").append(lowerHull[i]).append(" y=").append(y[lowerHull[i]]).append("\n");
    }
  }
}