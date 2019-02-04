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
package de.lmu.ifi.dbs.elki.math.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Classes to compute the convex hull of a set of points in 2D, using the
 * classic Grahams scan. Also computes a bounding box.
 * <p>
 * Reference:
 * <p>
 * P. Graham<br>
 * An Efficient Algorithm for Determining the Convex Hull of a Finite Planar
 * Set<br>
 * Information Processing Letters 1
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Polygon
 */
@Reference(authors = "P. Graham", //
    title = "An Efficient Algorithm for Determining the Convex Hull of a Finite Planar Set", //
    booktitle = "Information Processing Letters 1", //
    url = "https://doi.org/10.1016/0020-0190(72)90045-2", //
    bibkey = "DBLP:journals/ipl/Graham72")
public class GrahamScanConvexHull2D {
  /**
   * The current set of points
   */
  private List<double[]> points;

  /**
   * Min/Max in X
   */
  private DoubleMinMax minmaxX = new DoubleMinMax();

  /**
   * Min/Max in Y
   */
  private DoubleMinMax minmaxY = new DoubleMinMax();

  /**
   * Flag to indicate that the hull has been computed.
   */
  private boolean ok = false;

  /**
   * Scaling factor if we have very small polygons.
   *
   * TODO: needed? Does this actually improve things?
   */
  private double factor = 1.0;

  /**
   * Constructor.
   */
  public GrahamScanConvexHull2D() {
    this.points = new ArrayList<>();
  }

  /**
   * Add a single point to the list (this does not compute the hull!)
   *
   * @param point Point to add
   */
  public void add(double... point) {
    if(this.ok) {
      this.points = new ArrayList<>(this.points);
      this.ok = false;
    }
    this.points.add(point);
    // Update data set extends
    minmaxX.put(point[0]);
    minmaxY.put(point[1]);
  }

  /**
   * Compute the convex hull.
   */
  private void computeConvexHull() {
    // Trivial cases
    if(points.size() < 3) {
      this.ok = true;
      return;
    }
    // Avoid numerical instabilities by rescaling
    double maxX = Math.max(Math.abs(minmaxX.getMin()), Math.abs(minmaxX.getMax()));
    double maxY = Math.max(Math.abs(minmaxY.getMin()), Math.abs(minmaxY.getMax()));
    if(maxX < 10.0 || maxY < 10.0) {
      factor = 10 / maxX;
      if(10 / maxY > factor) {
        factor = 10 / maxY;
      }
    }
    // Find the new origin point
    findStartingPoint();
    // Sort points for the scan
    final double[] origin = points.get(0);
    Collections.sort(this.points, new Comparator<double[]>() {
      @Override
      public int compare(double[] o1, double[] o2) {
        return isLeft(o1, o2, origin);
      }
    });
    grahamScan();
    this.ok = true;
  }

  /**
   * Find the starting point, and sort it to the beginning of the list. The
   * starting point must be on the outer hull. Any "most extreme" point will do,
   * e.g. the one with the lowest Y coordinate and for ties with the lowest X.
   */
  private void findStartingPoint() {
    // Well, we already know the best Y value...
    final double bestY = minmaxY.getMin();
    double bestX = Double.POSITIVE_INFINITY;
    int bestI = -1;
    Iterator<double[]> iter = this.points.iterator();
    for(int i = 0; iter.hasNext(); i++) {
      double[] vec = iter.next();
      if(vec[1] == bestY && vec[0] < bestX) {
        bestX = vec[0];
        bestI = i;
      }
    }
    assert (bestI >= 0);
    // Bring the reference object to the head.
    if(bestI > 0) {
      points.add(0, points.remove(bestI));
    }
  }

  /**
   * Get the relative X coordinate to the origin.
   *
   * @param a
   * @param origin origin double[]
   * @return relative X coordinate
   */
  private double getRX(double[] a, double[] origin) {
    return (a[0] - origin[0]) * factor;
  }

  /**
   * Get the relative Y coordinate to the origin.
   *
   * @param a
   * @param origin origin double[]
   * @return relative Y coordinate
   */
  private double getRY(double[] a, double[] origin) {
    return (a[1] - origin[1]) * factor;
  }

  /**
   * Test whether a point is left of the other wrt. the origin.
   *
   * @param a double[] A
   * @param b double[] B
   * @param o Origin double[]
   * @return +1 when left, 0 when same, -1 when right
   */
  protected final int isLeft(double[] a, double[] b, double[] o) {
    final double cross = getRX(a, o) * getRY(b, o) - getRY(a, o) * getRX(b, o);
    if(cross == 0) {
      // Compare manhattan distances - same angle!
      final double dista = Math.abs(getRX(a, o)) + Math.abs(getRY(a, o));
      final double distb = Math.abs(getRX(b, o)) + Math.abs(getRY(b, o));
      return Double.compare(dista, distb);
    }
    return Double.compare(cross, 0);
  }

  /**
   * Manhattan distance.
   *
   * @param a double[] A
   * @param b double[] B
   * @return Manhattan distance
   */
  private double mdist(double[] a, double[] b) {
    return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
  }

  /**
   * Simple convexity test.
   *
   * @param a double[] A
   * @param b double[] B
   * @param c double[] C
   * @return convexity
   */
  private boolean isConvex(double[] a, double[] b, double[] c) {
    // We're using factor to improve numerical contrast for small polygons.
    double area = (b[0] - a[0]) * factor * (c[1] - a[1]) - (c[0] - a[0]) * factor * (b[1] - a[1]);
    return (-1e-13 < area && area < 1e-13) ? (mdist(b, c) > mdist(a, b) + mdist(a, c)) : (area < 0);
  }

  /**
   * The actual graham scan main loop.
   */
  private void grahamScan() {
    if(points.size() < 3) {
      return;
    }
    Iterator<double[]> iter = points.iterator();
    Stack<double[]> stack = new Stack<>();
    // Start with the first two points on the stack
    final double[] first = iter.next();
    stack.add(first);
    while(iter.hasNext()) {
      double[] n = iter.next();
      if(mdist(first, n) > 0) {
        stack.add(n);
        break;
      }
    }
    while(iter.hasNext()) {
      double[] next = iter.next();
      double[] curr = stack.pop();
      double[] prev = stack.peek();
      while((stack.size() > 1) && (mdist(curr, next) == 0 || !isConvex(prev, curr, next))) {
        curr = stack.pop();
        prev = stack.peek();
      }
      stack.add(curr);
      stack.add(next);
    }
    points = stack;
  }

  /**
   * Compute the convex hull, and return the resulting polygon.
   *
   * @return Polygon of the hull
   */
  public Polygon getHull() {
    if(!ok) {
      computeConvexHull();
    }
    return new Polygon(points, minmaxX.getMin(), minmaxX.getMax(), minmaxY.getMin(), minmaxY.getMax());
  }
}