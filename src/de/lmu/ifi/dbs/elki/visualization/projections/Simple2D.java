package de.lmu.ifi.dbs.elki.visualization.projections;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;

/**
 * Dimension-selecting 2D projection.
 * 
 * @author Erich Schubert
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
   * @param scales Scales to use
   * @param ax1 First axis
   * @param ax2 Second axis
   */
  public Simple2D(LinearScale[] scales, int ax1, int ax2) {
    super(scales);
    this.dim1 = ax1 - 1;
    this.dim2 = ax2 - 1;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(double[] data) {
    double x = (scales[dim1].getScaled(data[dim1]) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data[dim2]) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector<?> data) {
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
  public double[] fastProjectDataToScaledSpace(NumberVector<?> data) {
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
  public double[] fastProjectRenderToDataSpace(double[] v) {
    double[] ret = new double[scales.length];
    for(int d = 0; d < scales.length; d++) {
      if(d == dim1) {
        ret[d] = scales[d].getUnscaled((v[0] / SCALE) + 0.5);
      }
      else if(d == dim2) {
        ret[d] = scales[d].getUnscaled((v[1] / -SCALE) + 0.5);
      }
      else {
        ret[d] = scales[d].getUnscaled(0.5);
      }
    }
    return ret;
  }

  @Override
  public double[] fastProjectRenderToScaledSpace(double[] v) {
    double[] ret = new double[scales.length];
    for(int d = 0; d < scales.length; d++) {
      if(d == dim1) {
        ret[d] = (v[0] / SCALE) + 0.5;
      }
      else if(d == dim2) {
        ret[d] = (v[1] / -SCALE) + 0.5;
      }
      else {
        ret[d] = 0.5;
      }
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
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector<?> data) {
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
  public BitSet getVisibleDimensions2D() {
    BitSet actDim = new BitSet();
    actDim.set(dim1);
    actDim.set(dim2);
    return actDim;
  }

  @Override
  public CanvasSize estimateViewport() {
    return new CanvasSize(-SCALE * .5, SCALE * .5, -SCALE * .5, SCALE * .5);
  }

  @Override
  protected Vector rearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    r[0] = s[dim1];
    r[1] = s[dim2];
    final int ldim = Math.min(dim1, dim2);
    final int hdim = Math.max(dim1, dim2);
    if(ldim > 0) {
      System.arraycopy(s, 0, r, 2, ldim);
    }
    if(hdim - ldim > 1) {
      System.arraycopy(s, ldim + 1, r, ldim + 2, hdim - (ldim + 1));
    }
    if(hdim + 1 < s.length) {
      System.arraycopy(s, hdim + 1, r, hdim + 1, s.length - (hdim + 1));
    }
    return new Vector(r);
  }

  @Override
  protected Vector dearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    r[dim1] = s[0];
    r[dim2] = s[1];
    // copy remainder
    final int ldim = Math.min(dim1, dim2);
    final int hdim = Math.max(dim1, dim2);
    if(ldim > 0) {
      System.arraycopy(s, 2, r, 0, ldim);
    }
    // ldim = s[0 or 1]
    if(hdim - ldim > 1) {
      System.arraycopy(s, ldim + 2, r, ldim + 1, hdim - (ldim + 1));
    }
    // hdim = s[0 or 1]
    if(hdim + 1 < s.length) {
      System.arraycopy(s, hdim + 1, r, hdim + 1, s.length - (hdim + 1));
    }
    return new Vector(r);
  }
}