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
import java.util.Arrays;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.result.Result;

/**
 * An XYCurve is an ordered collection of 2d {@link Curve}s, meant for chart
 * generation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - Curve
 */
public class XYPlot implements Result, Iterable<XYPlot.Curve> {
  /**
   * Simplification threshold
   */
  protected static final double THRESHOLD = 1E-13;

  /**
   * Curves on this plot.
   */
  protected ArrayList<Curve> curves = new ArrayList<Curve>();

  /**
   * Curve on this plot.
   *
   * @author Erich Schubert
   * 
   * @has - - - Itr
   */
  public class Curve {
    /**
     * X and Y positions
     */
    protected double[] data;

    /**
     * Size.
     */
    protected int len = 0;

    /**
     * Suggested color (number).
     */
    protected int color;

    /**
     * Constructor.
     *
     * @param color Color number (curve number)
     */
    public Curve(int color) {
      this.color = color;
      this.data = new double[32];
    }

    /**
     * Constructor.
     *
     * @param color Color number (curve number)
     * @param size Expected size
     */
    public Curve(int color, int size) {
      this.color = color;
      this.data = new double[size << 1];
    }

    /**
     * Suggested color number.
     *
     * @return Color number
     */
    public int getColor() {
      return color;
    }

    /**
     * Add a coordinate pair, but don't simplify
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void add(double x, double y) {
      if(len == data.length) {
        data = Arrays.copyOf(data, len << 1);
      }
      data[len++] = x;
      data[len++] = y;
      minx = Math.min(minx, x);
      maxx = Math.max(maxx, x);
      miny = Math.min(miny, y);
      maxy = Math.max(maxy, y);
    }

    /**
     * Curve X value at given position
     *
     * @param off Offset
     * @return X value
     */
    public double getX(int off) {
      return data[off << 1];
    }

    /**
     * Curve Y value at given position
     *
     * @param off Offset
     * @return Y value
     */
    public double getY(int off) {
      return data[(off << 1) + 1];
    }

    /**
     * Size of curve.
     *
     * @return curve length
     */
    public int size() {
      return len >> 1;
    }

    /**
     * Get an iterator for the curve.
     *
     * Note: this is <em>not</em> a Java style iterator, since the only way to
     * get positions is using "next" in Java style. Here, we can have two
     * getters for current values!
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
        return data[pos];
      }

      /**
       * Get y value of current element.
       *
       * @return Y value of current element
       */
      public double getY() {
        return data[pos + 1];
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
        return pos < len;
      }
    }
  }

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
  public XYPlot(String labelx, String labely) {
    super();
    this.labelx = labelx;
    this.labely = labely;
  }

  /**
   * Constructor.
   */
  public XYPlot() {
    this("X", "Y");
  }

  /**
   * Make a new curve.
   *
   * @return Curve
   */
  public Curve makeCurve() {
    Curve c = new Curve(curves.size());
    curves.add(c);
    return c;
  }

  /**
   * Make a new curve with desired color.
   *
   * @param color Color number
   * @return Curve
   */
  public Curve makeCurve(int color) {
    Curve c = new Curve(color);
    curves.add(c);
    return c;
  }

  /**
   * Make a new curve with desired color.
   *
   * @param color Color number
   * @param size Expected size
   * @return Curve
   */
  public Curve makeCurve(int color, int size) {
    Curve c = new Curve(color, size);
    curves.add(c);
    return c;
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

  @Override
  public Iterator<Curve> iterator() {
    return curves.iterator();
  }

  @Override
  public String getLongName() {
    return labelx + "-" + labely + "-Curve";
  }

  @Override
  public String getShortName() {
    return (labelx + "-" + labely + "-curve").toLowerCase();
  }
}
