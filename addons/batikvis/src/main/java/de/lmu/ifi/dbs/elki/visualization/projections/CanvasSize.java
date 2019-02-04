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
package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.math.MathUtil;
/**
 * Size of a canvas. A 2D bounding rectangle.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class CanvasSize {
  /**
   * Minimum X
   */
  public final double minx;

  /**
   * Maximum X
   */
  public final double maxx;

  /**
   * Minimum Y
   */
  public final double miny;

  /**
   * Maximum Y
   */
  public final double maxy;

  /**
   * Constructor.
   * 
   * @param minx Minimum X
   * @param maxx Maximum X
   * @param miny Minimum Y
   * @param maxy Maximum Y
   */
  public CanvasSize(double minx, double maxx, double miny, double maxy) {
    super();
    this.minx = minx;
    this.maxx = maxx;
    this.miny = miny;
    this.maxy = maxy;
  }

  /**
   * @return the mininum X
   */
  public double getMinX() {
    return minx;
  }

  /**
   * @return the maximum X
   */
  public double getMaxX() {
    return maxx;
  }

  /**
   * @return the minimum Y
   */
  public double getMinY() {
    return miny;
  }

  /**
   * @return the maximum Y
   */
  public double getMaxY() {
    return maxy;
  }

  /**
   * @return the length on X
   */
  public double getDiffX() {
    return maxx - minx;
  }

  /**
   * @return the length on Y
   */
  public double getDiffY() {
    return maxy - miny;
  }

  /**
   * Continue a line along a given direction to the margin.
   * 
   * @param origin Origin point
   * @param delta Direction vector
   * @return scaling factor for delta vector
   */
  public double continueToMargin(double[] origin, double[] delta) {
    assert (delta.length == 2 && origin.length == 2);
    double factor = Double.POSITIVE_INFINITY;
    if(delta[0] > 0) {
      factor = Math.min(factor, (maxx - origin[0]) / delta[0]);
    }
    else if(delta[0] < 0) {
      factor = Math.min(factor, (origin[0] - minx) / -delta[0]);
    }
    if(delta[1] > 0) {
      factor = Math.min(factor, (maxy - origin[1]) / delta[1]);
    }
    else if(delta[1] < 0) {
      factor = Math.min(factor, (origin[1] - miny) / -delta[1]);
    }
    return factor;
  }

  /**
   * Clip a line on the margin (modifies arrays!)
   * 
   * @param origin Origin point, <b>will be modified</b>
   * @param target Target point, <b>will be modified</b>
   * @return {@code false} if entirely outside the margin
   */
  public boolean clipToMargin(double[] origin, double[] target) {
    assert (target.length == 2 && origin.length == 2);
    if ((origin[0] < minx && target[0] < minx) //
        || (origin[0] > maxx && target[0] > maxx) //
        || (origin[1] < miny && target[1] < miny) //
        || (origin[1] > maxy && target[1] > maxy)) {
      return false;
    }
    double deltax = target[0] - origin[0];
    double deltay = target[1] - origin[1];
    double fmaxx = (maxx - origin[0]) / deltax;
    double fmaxy = (maxy - origin[1]) / deltay;
    double fminx = (minx - origin[0]) / deltax;
    double fminy = (miny - origin[1]) / deltay;
    double factor1 = MathUtil.min(1, fmaxx > fminx ? fmaxx : fminx, fmaxy > fminy ? fmaxy : fminy);
    double factor2 = MathUtil.max(0, fmaxx < fminx ? fmaxx : fminx, fmaxy < fminy ? fmaxy : fminy);
    if (factor1 <= factor2) {
      return false; // Clipped!
    }
    target[0] = origin[0] + factor1 * deltax;
    target[1] = origin[1] + factor1 * deltay;
    origin[0] += factor2 * deltax;
    origin[1] += factor2 * deltay;
    return true;
  }

  @Override
  public String toString() {
    return "CanvasSize[x=" + minx + ":" + maxx + ", y=" + miny + ":" + maxy + "]";
  }
}