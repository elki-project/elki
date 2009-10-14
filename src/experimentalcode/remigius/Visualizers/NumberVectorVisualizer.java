package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import experimentalcode.erich.visualization.VisualizationProjection;

/**
 * Abstract superclass for Visualizers which process NumberVectors.
 * 
 * @author Remigius Wojdanowski
 * 
 *         TODO: Add missing documentation for the parameters. This also
 *         includes (all) sub-classes.
 * 
 * @param <NV>
 */
public abstract class NumberVectorVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer<NV> {
  /**
   * Projection used
   */
  protected VisualizationProjection<NV> proj = null;
  
  /**
   * Setup the projection used by this visualizer.
   * 
   * @param proj Projection
   */
  public void setup(VisualizationProjection<NV> proj) {
    this.proj = proj;
  }

  /**
   * Returns a Double representing the position where the object will be placed.
   * 
   * @see #getProjected(NumberVector, int)
   * 
   * @param o the object to be positioned.
   * @param dim the dimension in which the position will be calculated.
   * @return a Double representing the scaled position of the object in the
   *         given dimension.
   */
  public Double getProjected(NV nv, int dim) {
    Vector v = proj.projectDataToRenderSpace(nv);
    return v.get(dim - 1);
  }

  /**
   * Returns a Double representing a given coordinate being scaled
   * 
   * @param n the coordinate to scale.
   * @param dim the dimension indicating the scale to be used.
   * @return a Double representing a given coordinate being scaled appropriately
   *         to our actual coordinate system.
   */
  public Double getScaled(Number n, int dim) {
    return proj.getScale(dim).getScaled(n.doubleValue());
  }
}
