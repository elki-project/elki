package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

/**
 * Abstract base projection class.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractProjection implements Projection {
  /**
   * Scales in data set
   */
  final protected LinearScale[] scales;
  
  /**
   * Constructor.
   * 
   * @param scales Scales to use
   */
  public AbstractProjection(LinearScale[] scales) {
    super();
    this.scales = scales;
  }

  /**
   * Get the scales used, for rendering scales mostly.
   * 
   * @param d Dimension
   * @return Scale used
   */
  @Override
  public LinearScale getScale(int d) {
    return scales[d];
  }

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  @Override
  public Vector projectDataToScaledSpace(NumberVector<?, ?> data) {
    final int dim = data.getDimensionality();
    Vector vec = new Vector(dim);
    double[] ds = vec.getArrayRef();
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getScaled(data.doubleValue(d + 1));
    }
    return vec;
  }

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  @Override
  public Vector projectDataToScaledSpace(Vector data) {
    double[] src = data.getArrayRef();
    final int dim = src.length;
    double[] dst = new double[dim];
    for(int d = 0; d < dim; d++) {
      dst[d] = scales[d].getScaled(src[d]);
    }
    return new Vector(dst);
  }

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  @Override
  public Vector projectRelativeDataToScaledSpace(NumberVector<?, ?> data) {
    final int dim = data.getDimensionality();
    Vector vec = new Vector(dim);
    double[] ds = vec.getArrayRef();
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getRelativeScaled(data.doubleValue(d + 1));
    }
    return vec;
  }

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  @Override
  public Vector projectRelativeDataToScaledSpace(Vector data) {
    double[] src = data.getArrayRef();
    final int dim = src.length;
    double[] dst = new double[dim];
    for(int d = 0; d < dim; d++) {
      dst[d] = scales[d].getRelativeScaled(src[d]);
    }
    return new Vector(dst);
  }

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  @Override
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  @Override
  public Vector projectDataToRenderSpace(Vector data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  @Override
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  @Override
  public Vector projectRelativeDataToRenderSpace(Vector data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /**
   * Project a vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in scaled space
   * @param factory Object factory
   * @return vector in data space
   */
  @Override
  public <NV extends NumberVector<NV, ?>> NV projectScaledToDataSpace(Vector v, NV factory) {
    final int dim = v.getDimensionality();
    Vector vec = v.copy();
    double[] ds = vec.getArrayRef();
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getUnscaled(ds[d]);
    }
    return factory.newInstance(vec);
  }

  /**
   * Project a vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in rendering space
   * @param prototype Object factory
   * @return vector in data space
   */
  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getUnscaled(ds[d]);
    }
    return prototype.newInstance(vec);
  }

  /**
   * Project a relative vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in scaled space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeScaledToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = v.copy();
    double[] ds = vec.getArrayRef();
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getRelativeUnscaled(ds[d]);
    }
    return prototype.newInstance(vec);
  }

  /**
   * Project a relative vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in rendering space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRelativeRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getRelativeUnscaled(ds[d]);
    }
    return prototype.newInstance(vec);
  }
}