package de.lmu.ifi.dbs.elki.visualization.projections;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

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
  public double[] fastProjectDataToRenderSpace(Vector data) {
    double x = (scales[dim1].getScaled(data.get(dim1)) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data.get(dim2)) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data) {
    double x = (scales[dim1].getScaled(data.doubleValue(dim1 + 1)) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data.doubleValue(dim2 + 1)) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectDataToRenderSpace(double[] data) {
    double x = (scales[dim1].getScaled(data[dim1]) - 0.5) * SCALE;
    double y = (scales[dim2].getScaled(data[dim2]) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectScaledToRender(Vector v) {
    double x = (v.get(dim1) - 0.5) * SCALE;
    double y = (v.get(dim2) - 0.5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectScaledToRender(double[] vr) {
    double x = (vr[dim1] - .5) * SCALE;
    double y = (vr[dim2] - .5) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(Vector data) {
    double x = scales[dim1].getRelativeScaled(data.get(dim1)) * SCALE;
    double y = scales[dim2].getRelativeScaled(data.get(dim2)) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    double x = scales[dim1].getRelativeScaled(data.doubleValue(dim1 + 1)) * SCALE;
    double y = scales[dim2].getRelativeScaled(data.doubleValue(dim2 + 1)) * -SCALE;
    return new double[] { x, y };
  }

  @Override
  public double[] fastProjectRelativeScaledToRender(Vector v) {
    final double[] vr = v.getArrayRef();
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
  public Pair<MinMax<Double>, MinMax<Double>> estimateViewport() {
    MinMax<Double> minmaxx = new MinMax<Double>();
    MinMax<Double> minmaxy = new MinMax<Double>();
    minmaxx.put(SCALE * .5);
    minmaxx.put(-SCALE * .5);
    minmaxy.put(SCALE * .5);
    minmaxy.put(-SCALE * .5);
    return new Pair<MinMax<Double>, MinMax<Double>>(minmaxx, minmaxy);
  }

  @Override
  public String estimateTransformString(double margin, double width, double height) {
    Pair<MinMax<Double>, MinMax<Double>> minmax = estimateViewport();
    double sizex = (minmax.first.getMax() - minmax.first.getMin());
    double sizey = (minmax.second.getMax() - minmax.second.getMin());
    return SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin) + " translate(" + SVGUtil.fmt(sizex / 2) + " " + SVGUtil.fmt(sizey / 2) + ")";
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
    if (hdim - ldim > 1) {
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
    if (hdim - ldim > 1) {
      System.arraycopy(s, ldim + 2, r, ldim + 1, hdim - (ldim + 1));
    }
    // hdim = s[0 or 1]
    if(hdim + 1 < s.length) {
      System.arraycopy(s, hdim + 1, r, hdim + 1, s.length - (hdim + 1));
    }
    return new Vector(r);
  }
}