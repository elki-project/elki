package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

/**
 * Dimension-selecting 1D projection.
 * 
 * @author Erich Schubert
 */
public class Simple1D extends AbstractSimpleProjection implements Projection1D {
  /**
   * Our dimension, starting with 0
   */
  final int dnum;

  /**
   * Simple 1D projection using scaling only.
   * 
   * @param scales Scales to use
   * @param dnum Dimension (starting at 1)
   */
  public Simple1D(LinearScale[] scales, int dnum) {
    super(scales);
    this.dnum = dnum - 1;
  }

  @Override
  public double fastProjectDataToRenderSpace(Vector data) {
    return fastProjectDataToRenderSpace(data.getArrayRef());
  }

  @Override
  public double fastProjectDataToRenderSpace(NumberVector<?, ?> data) {
    return (scales[dnum].getScaled(data.doubleValue(dnum + 1)) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectDataToRenderSpace(double[] data) {
    return (scales[dnum].getScaled(data[dnum]) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectScaledToRender(Vector v) {
    return fastProjectScaledToRender(v.getArrayRef());
  }

  @Override
  public double fastProjectScaledToRender(double[] v) {
    return (v[dnum] - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(double[] data) {
    return (scales[dnum].getScaled(data[dnum]) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(Vector data) {
    return fastProjectRelativeDataToRenderSpace(data.getArrayRef());
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return (data.doubleValue(dnum) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeScaledToRender(Vector v) {
    return fastProjectRelativeScaledToRender(v.getArrayRef());
  }

  @Override
  public double fastProjectRelativeScaledToRender(double[] d) {
    return d[dnum] * SCALE;
  }

  @Override
  protected Vector rearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    r[0] = s[dnum];
    if(dnum > 0) {
      System.arraycopy(s, 0, r, 1, dnum);
    }
    if(dnum + 1 < s.length) {
      System.arraycopy(s, dnum + 1, r, dnum + 1, s.length - (dnum + 1));
    }
    return new Vector(r);
  }

  @Override
  protected Vector dearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    if(dnum > 0) {
      System.arraycopy(s, 1, r, 0, dnum);
    }
    r[dnum] = s[0];
    if(dnum + 1 < s.length) {
      System.arraycopy(s, dnum + 1, r, dnum + 1, s.length - (dnum + 1));
    }
    return new Vector(r);
  }
}