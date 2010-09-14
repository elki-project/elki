package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Base interface used for projections in the ELKI visualizers.
 * 
 * There are specialized interfaces for 1D and 2D that only compute the
 * projections in the required dimensions!
 * 
 * @author Erich Schubert
 */
public interface Projection {
  /**
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary#SCALE}.
   */
  public static final double SCALE = StyleLibrary.SCALE;

  /**
   * Get the scale class for a particular dimension.
   * 
   * @param d Dimension
   * @return Scale class
   */
  public LinearScale getScale(int d);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public Vector projectScaledToRender(Vector v);

  /**
   * Project a vector from rendering space to scaled space.
   * 
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  public Vector projectRenderToScaled(Vector v);

  /**
   * Project a relative vector from scaled space to rendering space.
   * 
   * @param v relative vector in scaled space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeScaledToRender(Vector v);

  /**
   * Project a relative vector from rendering space to scaled space.
   * 
   * @param v relative vector in rendering space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeRenderToScaled(Vector v);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(NumberVector<?, ?> data);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(Vector data);

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(NumberVector<?, ?> data);

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(Vector data);

  /**
   * Project a vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in scaled space
   * @param factory Object factory
   * @return vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectScaledToDataSpace(Vector v, NV factory);

  /**
   * Project a vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in rendering space
   * @param prototype Object factory
   * @return vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype);

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(Vector data);

  /**
   * Project a relative vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in scaled space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRelativeScaledToDataSpace(Vector v, NV prototype);

  /**
   * Project a relative vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in rendering space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype);
}