package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

/**
 * Abstract base class for "simple" projections.
 * 
 * Simple projections use the given scaling and dimension selection only.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractSimpleProjection extends AbstractProjection {
  /**
   * Constructor.
   * 
   * @param scales Scales to use
   */
  public AbstractSimpleProjection(LinearScale[] scales) {
    super(scales);
  }

  @Override
  public Vector projectScaledToRender(Vector v) {
    v = rearrange(v);
    v = v.minusEquals(.5);
    v = flipSecondEquals(v);
    v = v.timesEquals(SCALE);
    return v;
  }

  @Override
  public Vector projectRenderToScaled(Vector v) {
    v = v.times(1. / SCALE);
    v = flipSecondEquals(v);
    v = v.plusEquals(.5);
    v = dearrange(v);
    return v;
  }

  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    v = rearrange(v);
    v = flipSecondEquals(v);
    v = v.timesEquals(SCALE);
    return v;
  }

  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    v = v.times(1. / SCALE);
    v = flipSecondEquals(v);
    v = dearrange(v);
    return v;
  }

  /**
   * Flip the y axis.
   * 
   * @param v Vector
   * @return modified v
   */
  protected Vector flipSecondEquals(Vector v) {
    if(v.getDimensionality() > 1) {
      v.getArrayRef()[1] *= -1;
    }
    return v;
  }

  /**
   * Method to rearrange components
   * 
   * @param v Vector to rearrange
   * @return rearranged copy
   */
  protected abstract Vector rearrange(Vector v);

  /**
   * Undo the rearrangement of components
   * 
   * @param v Vector to undo the rearrangement
   * @return rearranged-undone copy
   */
  protected abstract Vector dearrange(Vector v);
}