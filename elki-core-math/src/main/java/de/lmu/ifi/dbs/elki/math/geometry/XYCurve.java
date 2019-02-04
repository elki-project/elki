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

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;

/**
 * An XYCurve is an ordered collection of 2d points, meant for chart generation.
 * Of key interest is the method {@link #addAndSimplify} which tries to simplify
 * the curve while adding points.
 *
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @has - - - Itr
 */
public class XYCurve implements Result {
  /**
   * Simplification threshold
   */
  protected static final double THRESHOLD = 1E-13;

  /**
   * X and Y positions
   */
  protected DoubleArray data;

  /**
   * Label of X axis
   */
  protected String labelx;

  /**
   * Label of Y axis
   */
  protected String labely;

  /**
   * Minimum and maximum for X axis
   */
  protected double minx = Double.POSITIVE_INFINITY,
      maxx = Double.NEGATIVE_INFINITY;

  /**
   * Minimum and maximum for Y axis
   */
  protected double miny = Double.POSITIVE_INFINITY,
      maxy = Double.NEGATIVE_INFINITY;

  /**
   * Constructor with labels
   *
   * @param labelx Label for X axis
   * @param labely Label for Y axis
   */
  public XYCurve(String labelx, String labely) {
    super();
    this.data = new DoubleArray();
    this.labelx = labelx;
    this.labely = labely;
  }

  /**
   * Constructor with size estimate and labels.
   *
   * @param labelx Label for X axis
   * @param labely Label for Y axis
   * @param size Estimated size (initial allocation size)
   */
  public XYCurve(String labelx, String labely, int size) {
    super();
    this.data = new DoubleArray(size << 1);
    this.labelx = labelx;
    this.labely = labely;
  }

  /**
   * Constructor.
   */
  public XYCurve() {
    this("X", "Y");
  }

  /**
   * Constructor with size estimate
   *
   * @param size Estimated size (initial allocation size)
   */
  public XYCurve(int size) {
    this("X", "Y", size);
  }

  /**
   * Constructor, cloning an existing curve.
   *
   * @param curve Curve to clone.
   */
  public XYCurve(XYCurve curve) {
    super();
    this.data = new DoubleArray(curve.data);
    this.labelx = curve.labelx;
    this.labely = curve.labely;
    this.minx = curve.minx;
    this.maxx = curve.maxx;
    this.miny = curve.miny;
    this.maxy = curve.maxy;
  }

  /**
   * Add a coordinate pair, but don't simplify
   *
   * @param x X coordinate
   * @param y Y coordinate
   */
  public void add(double x, double y) {
    data.add(x);
    data.add(y);
    minx = Math.min(minx, x);
    maxx = Math.max(maxx, x);
    miny = Math.min(miny, y);
    maxy = Math.max(maxy, y);
  }

  /**
   * Add a coordinate pair, performing curve simplification if possible.
   *
   * @param x X coordinate
   * @param y Y coordinate
   */
  public void addAndSimplify(double x, double y) {
    // simplify curve when possible:
    final int len = data.size();
    if (len >= 4) {
      // Look at the previous 2 points
      final double l1x = data.get(len - 4);
      final double l1y = data.get(len - 3);
      final double l2x = data.get(len - 2);
      final double l2y = data.get(len - 1);
      // Differences:
      final double ldx = l2x - l1x;
      final double ldy = l2y - l1y;
      final double cdx = x - l2x;
      final double cdy = y - l2y;
      // X simplification
      if ((ldx == 0) && (cdx == 0)) {
        data.remove(len - 2, 2);
      }
      // horizontal simplification
      else if ((ldy == 0) && (cdy == 0)) {
        data.remove(len - 2, 2);
      }
      // diagonal simplification
      else if (ldy > 0 && cdy > 0) {
        if (Math.abs((ldx / ldy) - (cdx / cdy)) < THRESHOLD) {
          data.remove(len - 2, 2);
        }
      }
    }
    add(x, y);
  }

  /**
   * Get label of x axis
   *
   * @return label of x axis
   */
  public String getLabelx() {
    return labelx;
  }

  /**
   * Get label of y axis
   *
   * @return label of y axis
   */
  public String getLabely() {
    return labely;
  }

  /**
   * Minimum on x axis.
   *
   * @return Minimum on X
   */
  public double getMinx() {
    return minx;
  }

  /**
   * Maximum on x axis.
   *
   * @return Maximum on X
   */
  public double getMaxx() {
    return maxx;
  }

  /**
   * Minimum on y axis.
   *
   * @return Minimum on Y
   */
  public double getMiny() {
    return miny;
  }

  /**
   * Maximum on y axis.
   *
   * @return Maximum on Y
   */
  public double getMaxy() {
    return maxy;
  }

  /**
   * Curve X value at given position
   *
   * @param off Offset
   * @return X value
   */
  public double getX(int off) {
    return data.get(off << 1);
  }

  /**
   * Curve Y value at given position
   *
   * @param off Offset
   * @return Y value
   */
  public double getY(int off) {
    return data.get((off << 1) + 1);
  }

  /**
   * Rescale the graph.
   *
   * @param sx Scaling factor for X axis
   * @param sy Scaling factor for Y axis
   */
  public void rescale(double sx, double sy) {
    for (int i = 0; i < data.size(); i += 2) {
      data.set(i, sx * data.get(i));
      data.set(i + 1, sy * data.get(i + 1));
    }
    maxx *= sx;
    maxy *= sy;
  }

  /**
   * Size of curve.
   *
   * @return curve length
   */
  public int size() {
    return data.size() >> 1;
  }

  /**
   * Get an iterator for the curve.
   *
   * Note: this is <em>not</em> a Java style iterator, since the only way to get
   * positions is using "next" in Java style. Here, we can have two getters for
   * current values!
   *
   * Instead, use this style of iteration: <blockquote>
   *
   * <pre>
   * {@code
   * for (XYCurve.Itr it = curve.iterator(); it.valid(); it.advance()) {
   *   doSomethingWith(it.getX(), it.getY());
   * }
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @return Iterator
   */
  public Itr iterator() {
    return new Itr();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("XYCurve[");
    buf.append(labelx).append(',').append(labely).append(':');
    for (int pos = 0; pos < data.size(); pos += 2) {
      buf.append(' ').append(data.get(pos)).append(',').append(data.get(pos + 1));
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public String getLongName() {
    return labelx + "-" + labely + "-Curve";
  }

  @Override
  public String getShortName() {
    return (labelx + "-" + labely + "-curve").toLowerCase();
  }

  /**
   * Compute the area under curve for a curve
   * <em>monotonously increasing in X</em>. You might need to relate this to the
   * total area of the chart.
   *
   * @param curve Curve
   * @return Area under curve.
   */
  public static double areaUnderCurve(XYCurve curve) {
    DoubleArray data = curve.data;
    double prevx = data.get(0), prevy = data.get(1);
    if (prevx > curve.minx) {
      throw new UnsupportedOperationException("Curves must be monotone on X for areaUnderCurve to be valid.");
    }
    double area = 0.0;
    for (int pos = 2; pos < data.size(); pos += 2) {
      final double curx = data.get(pos), cury = data.get(pos + 1);
      if (prevx > curx) {
        throw new UnsupportedOperationException("Curves must be monotone on X for areaUnderCurve to be valid.");
      }
      area += (curx - prevx) * (prevy + cury) * .5; // .5 = mean Y
      prevx = curx;
      prevy = cury;
    }
    if (prevx < curve.maxx) {
      throw new UnsupportedOperationException("Curves must be complete on X for areaUnderCurve to be valid.");
    }
    return area;
  }

  /**
   * Iterator for the curve. 2D, does not follow Java collections style. The
   * reason is that we want to have {@code #getX()} and {@code #getY()}
   * operations, which does not work consistently with Java's
   * <code>next()</code> style of iterations.
   *
   * Instead, use this style of iteration: <blockquote>
   *
   * <pre>
   * {@code
   * for (XYCurve.Itr it = curve.iterator(); it.valid(); it.advance()) {
   *   doSomethingWith(it.getX(), it.getY());
   * }
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @author Erich Schubert
   */
  public class Itr {
    /**
     * Iterator position
     */
    protected int pos = 0;

    /**
     * Get x value of current element.
     *
     * @return X value of current element
     */
    public double getX() {
      return data.get(pos);
    }

    /**
     * Get y value of current element.
     *
     * @return Y value of current element
     */
    public double getY() {
      return data.get(pos + 1);
    }

    /**
     * Advance the iterator to the next position.
     */
    public void advance() {
      pos += 2;
    }

    /**
     * Test if the iterator can advance.
     *
     * @return True when the iterator can be advanced.
     */
    public boolean valid() {
      return pos < data.size();
    }
  }
}
