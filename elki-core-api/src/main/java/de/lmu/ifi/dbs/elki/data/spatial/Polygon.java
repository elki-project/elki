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
package de.lmu.ifi.dbs.elki.data.spatial;

import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;

/**
 * Class representing a simple polygon. While you can obviously store non-simple
 * polygons in this, note that many of the supplied methods will assume that the
 * polygons are simple.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - double[]
 */
public class Polygon implements SpatialComparable {
  /**
   * The actual points
   */
  private List<double[]> points;

  /**
   * Minimum values
   */
  private double[] min = null;

  /**
   * Maximum values
   */
  private double[] max = null;

  /**
   * Constructor.
   * 
   * @param points Polygon points
   */
  public Polygon(List<double[]> points) {
    super();
    assert (points instanceof RandomAccess);
    this.points = points;
    // Compute the bounds.
    if(!points.isEmpty()) {
      final Iterator<double[]> iter = points.iterator();
      final double[] first = iter.next();
      final int dim = first.length;
      min = first.clone();
      max = first.clone();
      while(iter.hasNext()) {
        double[] next = iter.next();
        for(int i = 0; i < dim; i++) {
          final double cur = next[i];
          min[i] = Math.min(min[i], cur);
          max[i] = Math.max(max[i], cur);
        }
      }
    }
  }

  /**
   * Constructor.
   * 
   * @param points Polygon points
   * @param minx Minimum x value
   * @param maxx Maximum x value
   * @param miny Minimum y value
   * @param maxy Maximum y value
   */
  public Polygon(List<double[]> points, double minx, double maxx, double miny, double maxy) {
    super();
    assert (points instanceof RandomAccess);
    this.points = points;
    this.min = new double[] { minx, miny };
    this.max = new double[] { maxx, maxy };
  }

  /**
   * Get an iterator to the double[] contents.
   * 
   * @return Iterator
   */
  public ArrayListIter<double[]> iter() {
    return new ArrayListIter<>(points);
  }

  /**
   * Append the polygon to the buffer.
   * 
   * @param buf Buffer to append to
   */
  public StringBuilder appendToBuffer(StringBuilder buf) {
    Iterator<double[]> iter = points.iterator();
    while(iter.hasNext()) {
      double[] data = iter.next();
      for(int i = 0; i < data.length; i++) {
        if(i > 0) {
          buf.append(',');
        }
        buf.append(data[i]);
      }
      if(iter.hasNext()) {
        buf.append(' ');
      }
    }
    return buf;
  }

  @Override
  public String toString() {
    return appendToBuffer(new StringBuilder(points.size() * 20)).toString();
  }

  /**
   * Get the polygon length.
   * 
   * @return Polygon length
   */
  public int size() {
    return points.size();
  }

  /**
   * Get a double[] by index.
   * 
   * @param idx Index to get
   * @return double[]
   */
  public double[] get(int idx) {
    return points.get(idx);
  }

  @Override
  public int getDimensionality() {
    return min.length;
  }

  @Override
  public double getMin(int dimension) {
    return min[dimension];
  }

  @Override
  public double getMax(int dimension) {
    return max[dimension];
  }

  /**
   * Test polygon orientation.
   * 
   * @return -1, 0, 1 for counterclockwise, undefined and clockwise.
   */
  public int testClockwise() {
    if(points.size() < 3) {
      return 0;
    }
    final int size = points.size();
    // Count the number of positive and negative oriented angles
    int c = 0;

    // TODO: faster when using an iterator?
    for(int i = 0; i < size; i++) {
      // Three consecutive points
      final int j = (i + 1) % size;
      final int k = (i + 2) % size;
      final double dxji = points.get(j)[0] - points.get(i)[0];
      final double dykj = points.get(k)[1] - points.get(j)[1];
      final double dyji = points.get(j)[1] - points.get(i)[1];
      final double dxkj = points.get(k)[0] - points.get(j)[0];
      final double z = (dxji * dykj) - (dyji * dxkj);
      if(z < 0) {
        c--;
      }
      else if(z > 0) {
        c++;
      }
    }
    return (c > 0) ? -1 : (c < 0) ? +1 : 0;
  }

  /**
   * Simple polygon intersection test.
   * <p>
   * FIXME: while this is found on some web pages as "solution" and satisfies or
   * needs, it clearly is not correct; not even for convex polygons: Consider a
   * cross where the two bars are made out of four vertices each. No vertex is
   * inside the other polygon, yet they intersect.
   * <p>
   * I knew this before writing this code, but this O(n) code was the simplest
   * thing to come up with, and it would work for our current data sets. A way
   * to fix this is to augment it with the obvious O(n*n) segment intersection
   * test. (Note that you will still need to test for point containment, since
   * the whole polygon could be contained in the other!)
   * 
   * @param other Other polygon
   * @return True when the polygons intersect
   */
  public boolean intersects2DIncomplete(Polygon other) {
    assert (this.getDimensionality() == 2);
    assert (other.getDimensionality() == 2);
    for(double[] v : this.points) {
      if(other.containsPoint2D(v)) {
        return true;
      }
    }
    for(double[] v : other.points) {
      if(this.containsPoint2D(v)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Point in polygon test, based on
   * 
   * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
   * 
   * by W. Randolph Franklin
   * 
   * @param v Point to test
   * @return True when contained.
   */
  public boolean containsPoint2D(double[] v) {
    assert (v.length == 2);
    final double testx = v[0];
    final double testy = v[1];
    boolean c = false;

    Iterator<double[]> it = points.iterator();
    double[] pre = points.get(points.size() - 1);
    while(it.hasNext()) {
      final double[] cur = it.next();
      final double curx = cur[0], cury = cur[1];
      final double prex = pre[0], prey = pre[1];
      if(((cury > testy) != (prey > testy))) {
        if((testx < (prex - curx) * (testy - cury) / (prey - cury) + curx)) {
          c = !c;
        }
      }
      pre = cur;
    }
    return c;
  }

  /**
   * Compute the polygon area (geometric, not geographic) using the Shoelace
   * formula.
   *
   * This is appropriate for simple polygons in the cartesian plane only.
   *
   * @return Area
   */
  public double areaShoelace() {
    if(points.size() <= 1) {
      return 0.;
    }
    double agg = 0.;
    Iterator<double[]> iter = points.iterator();
    double[] first = iter.next(), cur = null;
    double px = first[0], py = first[1];
    while(iter.hasNext()) {
      cur = iter.next();
      double x = cur[0], y = cur[1];
      agg += x * py - y * px;
      px = x;
      py = y;
    }
    agg += first[0] * py - first[1] * px;
    return Math.abs(agg * .5);
  }
}
