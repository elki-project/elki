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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * Affine projections are the most general class. They are initialized by an
 * arbitrary affine transformation matrix, and can thus represent any rotation
 * and scaling, even simple perspective projections.
 *
 * However, this comes at the cost of a matrix multiplication.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class AffineProjection extends AbstractFullProjection implements Projection2D {
  /**
   * Affine transformation used in projection
   */
  private AffineTransformation proj;

  /**
   * Viewport (cache)
   */
  private CanvasSize viewport = null;

  /**
   * Constructor with a given database and axes.
   *
   * @param p Projector
   * @param scales Scales to use
   * @param proj Projection to use
   */
  public AffineProjection(Projector p, LinearScale[] scales, AffineTransformation proj) {
    super(p, scales);
    this.proj = proj;
  }

  /**
   * Project a vector from scaled space to rendering space.
   *
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  @Override
  public double[] projectScaledToRender(double[] v) {
    return proj.apply(v);
  }

  /**
   * Project a vector from rendering space to scaled space.
   *
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  @Override
  public double[] projectRenderToScaled(double[] v) {
    return proj.applyInverse(v);
  }

  /**
   * Project a relative vector from scaled space to rendering space.
   *
   * @param v relative vector in scaled space
   * @return relative vector in rendering space
   */
  @Override
  public double[] projectRelativeScaledToRender(double[] v) {
    return proj.applyRelative(v);
  }

  /**
   * Project a relative vector from rendering space to scaled space.
   *
   * @param v relative vector in rendering space
   * @return relative vector in scaled space
   */
  @Override
  public double[] projectRelativeRenderToScaled(double[] v) {
    return proj.applyRelativeInverse(v);
  }

  @Override
  public CanvasSize estimateViewport() {
    if(viewport == null) {
      final int dim = proj.getDimensionality();
      DoubleMinMax minmaxx = new DoubleMinMax();
      DoubleMinMax minmaxy = new DoubleMinMax();

      // Origin
      final double[] vec = new double[dim];
      double[] orig = projectScaledToRender(vec);
      minmaxx.put(orig[0]);
      minmaxy.put(orig[1]);
      // Diagonal point
      Arrays.fill(vec, 1.);
      double[] diag = projectScaledToRender(vec);
      minmaxx.put(diag[0]);
      minmaxy.put(diag[1]);
      // Axis end points
      for(int d = 0; d < dim; d++) {
        Arrays.fill(vec, 0.);
        vec[d] = 1.;
        double[] ax = projectScaledToRender(vec);
        minmaxx.put(ax[0]);
        minmaxy.put(ax[1]);
      }
      viewport = new CanvasSize(minmaxx.getMin(), minmaxx.getMax(), minmaxy.getMin(), minmaxy.getMax());
    }
    return viewport;
  }

  /**
   * Compute an transformation matrix to show only axis ax1 and ax2.
   *
   * @param dim Dimensionality
   * @param ax1 First axis
   * @param ax2 Second axis
   * @return transformation matrix
   */
  public static AffineTransformation axisProjection(int dim, int ax1, int ax2) {
    // setup a projection to get the data into the interval -1:+1 in each
    // dimension with the intended-to-see dimensions first.
    AffineTransformation proj = AffineTransformation.reorderAxesTransformation(dim, ax1, ax2);
    // Assuming that the data was normalized on [0:1], center it:
    double[] trans = new double[dim];
    for(int i = 0; i < dim; i++) {
      trans[i] = -.5;
    }
    proj.addTranslation(trans);
    // mirror on the y axis, since the SVG coordinate system is screen
    // coordinates (y = down) and not mathematical coordinates (y = up)
    proj.addAxisReflection(2);
    // scale it up
    proj.addScaling(SCALE);

    return proj;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(double[] data) {
    return fastProjectScaledToRenderSpace(fastProjectDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector data) {
    return fastProjectScaledToRenderSpace(fastProjectDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectDataToScaledSpace(double[] data) {
    return projectDataToScaledSpace(data);
  }

  @Override
  public double[] fastProjectDataToScaledSpace(NumberVector data) {
    return projectDataToScaledSpace(data);
  }

  @Override
  public double[] fastProjectScaledToRenderSpace(double[] vr) {
    double x = 0.0;
    double y = 0.0;
    double s = 0.0;

    final double[][] matrix = proj.getTransformation();
    final double[] colx = matrix[0];
    final double[] coly = matrix[1];
    final double[] cols = matrix[vr.length];
    assert (colx.length == coly.length && colx.length == cols.length && cols.length == vr.length + 1);

    for(int k = 0; k < vr.length; k++) {
      x += colx[k] * vr[k];
      y += coly[k] * vr[k];
      s += cols[k] * vr[k];
    }
    // add homogene component:
    x += colx[vr.length];
    y += coly[vr.length];
    s += cols[vr.length];
    // Note: we may have NaN values here.
    // assert (s > 0.0 || s < 0.0);
    return new double[] { x / s, y / s };
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(double[] data) {
    return fastProjectRelativeScaledToRenderSpace(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector data) {
    // FIXME: implement with less objects?
    return fastProjectRelativeScaledToRenderSpace(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectRelativeScaledToRenderSpace(double[] vr) {
    double x = 0.0;
    double y = 0.0;

    final double[][] matrix = proj.getTransformation();
    final double[] colx = matrix[0];
    final double[] coly = matrix[1];
    assert (colx.length == coly.length);

    for(int k = 0; k < vr.length; k++) {
      x += colx[k] * vr[k];
      y += coly[k] * vr[k];
    }
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRenderToDataSpace(double x, double y) {
    double[] ret = fastProjectRenderToScaledSpace(x, y);
    for(int d = 0; d < scales.length; d++) {
      ret[d] = scales[d].getUnscaled(ret[d]);
    }
    return ret;
  }

  @Override
  public double[] fastProjectRenderToScaledSpace(double x, double y) {
    double[] c = new double[scales.length];
    c[0] = x;
    c[1] = y;
    Arrays.fill(c, 2, scales.length, 0.5);
    return projectRenderToScaled(c);
  }

  @Override
  public long[] getVisibleDimensions2D() {
    final int dim = proj.getDimensionality();
    long[] actDim = BitsUtil.zero(dim);
    double[] vScale = new double[dim];
    for(int d = 0; d < dim; d++) {
      Arrays.fill(vScale, 0);
      vScale[d] = 1;
      double[] vRender = fastProjectScaledToRenderSpace(vScale);

      // TODO: Can't we do this by inspecting the projection matrix directly?
      if(vRender[0] > 0.0 || vRender[0] < 0.0 || vRender[1] != 0) {
        BitsUtil.setI(actDim, d);
      }
    }
    return actDim;
  }
}
