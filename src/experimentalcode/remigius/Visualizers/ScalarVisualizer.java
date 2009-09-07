package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <NV>
 * @param <N>
 */
public abstract class ScalarVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends NumberVectorVisualizer<NV, N> {
  
  /**
   * the dimension to appear as horizontal dimension.
   */
  protected int dim;
  
  /**
   * Setting up parameters individual to each run of the visualization.
   * 
   * @param dimx
   * @param dimy
   */
  public void setup(int dim){
    this.dim = dim;
  }
}
