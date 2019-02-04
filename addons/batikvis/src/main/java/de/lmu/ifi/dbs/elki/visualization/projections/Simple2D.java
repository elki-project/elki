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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * Dimension-selecting 2D projection.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class Simple2D extends AbstractSimpleProjection implements Projection2D {
  /**
   * Dimensions for fast projection mode.
   */
  private int dim1;

  /**
   * Dimensions for fast projection mode.
   */
  private int dim2;

  /**
   * Constructor with a given database and axes.
   *
   * @param p Projector
   * @param scales Scales to use
   * @param ax1 First axis
   * @param ax2 Second axis
   */
  public Simple2D(Projector p, LinearScale[] scales, int ax1, int ax2) {
    super(p, scales);
    this.dim1 = ax1;
    this.dim2 = ax2;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(double[] data) {
    double x = (scales[dim1].getScaled(data[dim1]) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data[dim2]) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector data) {
    double x = (scales[dim1].getScaled(data.doubleValue(dim1)) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data.doubleValue(dim2)) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectDataToScaledSpace(double[] data) {
    final int dim = data.length;
    double[] ds = new double[dim];
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getScaled(data[d]);
    }
    return ds;
  }

  @Override
  public double[] fastProjectDataToScaledSpace(NumberVector data) {
    final int dim = data.getDimensionality();
    double[] ds = new double[dim];
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getScaled(data.doubleValue(d));
    }
    return ds;
  }

  @Override
  public double[] fastProjectScaledToRenderSpace(double[] v) {
    double x = (v[dim1] - 0.5) * SCALE;
    double y = (v[dim2] - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRenderToDataSpace(double x, double y) {
    double[] ret = new double[scales.length];
    for(int d = 0; d < scales.length; d++) {
      ret[d] = //
      (d == dim1) ? scales[d].getUnscaled((x * INVSCALE) + 0.5) : //
      (d == dim2) ? scales[d].getUnscaled((y * -INVSCALE) + 0.5) : //
      scales[d].getUnscaled(0.5);
    }
    return ret;
  }

  @Override
  public double[] fastProjectRenderToScaledSpace(double x, double y) {
    double[] ret = new double[scales.length];
    for(int d = 0; d < scales.length; d++) {
      ret[d] = //
      (d == dim1) ? (x * INVSCALE) + 0.5 : //
      (d == dim2) ? (y * -INVSCALE) + 0.5 : //
      0.5;
    }
    return ret;
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(double[] data) {
    double x = scales[dim1].getRelativeScaled(data[dim1]) * SCALE;
    double y = scales[dim2].getRelativeScaled(data[dim2]) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector data) {
    double x = scales[dim1].getRelativeScaled(data.doubleValue(dim1)) * SCALE;
    double y = scales[dim2].getRelativeScaled(data.doubleValue(dim2)) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRelativeScaledToRenderSpace(double[] vr) {
    double x = vr[dim1] * SCALE;
    double y = vr[dim2] * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public long[] getVisibleDimensions2D() {
    long[] actDim = new long[super.scales.length];
    BitsUtil.setI(actDim, dim1);
    BitsUtil.setI(actDim, dim2);
    return actDim;
  }

  @Override
  public CanvasSize estimateViewport() {
    return new CanvasSize(-SCALE * .5, SCALE * .5, -SCALE * .5, SCALE * .5);
  }

  @Override
  protected double[] rearrange(double[] v) {
    final double[] r = new double[v.length];
    r[0] = v[dim1];
    r[1] = v[dim2];
    final int ldim = Math.min(dim1, dim2);
    final int hdim = Math.max(dim1, dim2);
    if(ldim > 0) {
      System.arraycopy(v, 0, r, 2, ldim);
    }
    if(hdim - ldim > 1) {
      System.arraycopy(v, ldim + 1, r, ldim + 2, hdim - (ldim + 1));
    }
    if(hdim + 1 < v.length) {
      System.arraycopy(v, hdim + 1, r, hdim + 1, v.length - (hdim + 1));
    }
    return r;
  }

  @Override
  protected double[] dearrange(double[] v) {
    final double[] r = new double[v.length];
    r[dim1] = v[0];
    r[dim2] = v[1];
    // copy remainder
    final int ldim = Math.min(dim1, dim2);
    final int hdim = Math.max(dim1, dim2);
    if(ldim > 0) {
      System.arraycopy(v, 2, r, 0, ldim);
    }
    // ldim = s[0 or 1]
    if(hdim - ldim > 1) {
      System.arraycopy(v, ldim + 2, r, ldim + 1, hdim - (ldim + 1));
    }
    // hdim = s[0 or 1]
    if(hdim + 1 < v.length) {
      System.arraycopy(v, hdim + 1, r, hdim + 1, v.length - (hdim + 1));
    }
    return r;
  }

  @Override
  public String getMenuName() {
    return "Scatterplot";
  }
}