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
    return rearrange(v).minusEquals(-.5).timesEquals(SCALE);
  }

  @Override
  public Vector projectRenderToScaled(Vector v) {
    return dearrange(v).timesEquals(1./SCALE).plusEquals(.5);
  }

  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    return rearrange(v).minusEquals(-.5).timesEquals(SCALE);
  }

  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    return dearrange(v).timesEquals(1./SCALE).plusEquals(.5);
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