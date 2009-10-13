package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import experimentalcode.erich.visualization.VisualizationProjection;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <NV>
 */
public abstract class ScalarVisualizer<NV extends NumberVector<NV, ?>> extends NumberVectorVisualizer<NV> {
  /**
   * the dimension to appear as horizontal dimension.
   */
  protected int dim;
  
  /**
   * Setting up parameters individual to each run of the visualization.
   * 
   * @param dim
   */
  public void setup(int dim, VisualizationProjection<NV> proj){
    this.dim = dim;
    this.proj = proj;
  }
}
