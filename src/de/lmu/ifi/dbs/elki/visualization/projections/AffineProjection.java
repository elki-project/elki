package de.lmu.ifi.dbs.elki.visualization.projections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Affine projections are the most general class. They are initialized by an
 * arbitrary affine transformation matrix, and can thus represent any rotation
 * and scaling, even simple perspective projections.
 * 
 * However, this comes at the cost of a matrix multiplication.
 * 
 * @author Erich Schubert
 */
public class AffineProjection extends AbstractProjection implements Projection2D {
  /**
   * Affine transformation used in projection
   */
  private AffineTransformation proj;

  /**
   * Constructor with a given database and axes.
   * 
   * @param scales Scales to use
   * @param proj Projection to use
   */
  public AffineProjection(LinearScale[] scales, AffineTransformation proj) {
    super(scales);
    this.proj = proj;
  }

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  @Override
  public Vector projectScaledToRender(Vector v) {
    return proj.apply(v);
  }

  /**
   * Project a vector from rendering space to scaled space.
   * 
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  @Override
  public Vector projectRenderToScaled(Vector v) {
    return proj.applyInverse(v);
  }

  /**
   * Project a relative vector from scaled space to rendering space.
   * 
   * @param v relative vector in scaled space
   * @return relative vector in rendering space
   */
  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    return proj.applyRelative(v);
  }

  /**
   * Project a relative vector from rendering space to scaled space.
   * 
   * @param v relative vector in rendering space
   * @return relative vector in scaled space
   */
  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    return proj.applyRelativeInverse(v);
  }

  @Override
  public Pair<DoubleMinMax, DoubleMinMax> estimateViewport() {
    final int dim = proj.getDimensionality();
    DoubleMinMax minmaxx = new DoubleMinMax();
    DoubleMinMax minmaxy = new DoubleMinMax();

    // Origin
    Vector orig = new Vector(dim);
    orig = projectScaledToRender(orig);
    minmaxx.put(orig.get(0));
    minmaxy.put(orig.get(1));
    // Diagonal point
    Vector diag = new Vector(dim);
    for(int d2 = 0; d2 < dim; d2++) {
      diag.set(d2, 1);
    }
    diag = projectScaledToRender(diag);
    minmaxx.put(diag.get(0));
    minmaxy.put(diag.get(1));
    // Axis end points
    for(int d = 0; d < dim; d++) {
      Vector v = new Vector(dim);
      v.set(d, 1);
      Vector ax = projectScaledToRender(v);
      minmaxx.put(ax.get(0));
      minmaxy.put(ax.get(1));
    }
    return new Pair<DoubleMinMax, DoubleMinMax>(minmaxx, minmaxy);
  }

  @Override
  public String estimateTransformString(double margin, double width, double height) {
    Pair<DoubleMinMax, DoubleMinMax> minmax = estimateViewport();
    double sizex = (minmax.first.getMax() - minmax.first.getMin());
    double sizey = (minmax.second.getMax() - minmax.second.getMin());
    return SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin) + " translate(" + SVGUtil.fmt(sizex / 2) + " " + SVGUtil.fmt(sizey / 2) + ")";
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
    AffineTransformation proj = AffineTransformation.reorderAxesTransformation(dim, new int[] { ax1, ax2 });
    // Assuming that the data was normalized on [0:1], center it:
    double[] trans = new double[dim];
    for(int i = 0; i < dim; i++) {
      trans[i] = -.5;
    }
    proj.addTranslation(new Vector(trans));
    // mirror on the y axis, since the SVG coordinate system is screen
    // coordinates (y = down) and not mathematical coordinates (y = up)
    proj.addAxisReflection(2);
    // scale it up
    proj.addScaling(SCALE);

    return proj;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(Vector data) {
    return fastProjectScaledToRender(projectDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data) {
    return fastProjectScaledToRender(projectDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectScaledToRender(Vector v) {
    final double[] vr = v.getArrayRef();
    double x = 0.0;
    double y = 0.0;
    double s = 0.0;

    final double[][] matrix = proj.getTransformation().getArrayRef();
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
    assert (s != 0.0);
    return new double[] { x / s, y / s };
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(Vector data) {
    return fastProjectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return fastProjectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public double[] fastProjectRelativeScaledToRender(Vector v) {
    final double[] vr = v.getArrayRef();
    double x = 0.0;
    double y = 0.0;

    final double[][] matrix = proj.getTransformation().getArrayRef();
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
  public BitSet getVisibleDimensions2D() {
    final int dim = proj.getDimensionality();
    BitSet actDim = new BitSet(dim);
    Vector vScale = new Vector(dim);
    for(int d = 0; d < dim; d++) {
      vScale.setZero();
      vScale.set(d, 1);
      double[] vRender = fastProjectScaledToRender(vScale);

      // TODO: Can't we do this by inspecting the projection matrix directly?
      if(vRender[0] != 0.0 || vRender[1] != 0) {
        actDim.set(d);
      }
    }
    return actDim;
  }
}