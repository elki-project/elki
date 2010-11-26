package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Interface for projections that have a specialization to only compute the first component.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface Projection1D extends Projection {
  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double fastProjectDataToRenderSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double fastProjectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double fastProjectScaledToRender(Vector v);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double fastProjectRelativeDataToRenderSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double fastProjectRelativeScaledToRender(Vector v);
}