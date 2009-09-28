package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * Note that a PlanarVisualizer is <b>not</b> sub-classing ScalarVisualizer.
 * This only happens because use of the instanceof-operator to distinguish those
 * classes is slightly easier now.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 * @param <N>
 */
public abstract class PlanarVisualizer<NV extends NumberVector<NV, ?>> extends NumberVectorVisualizer<NV> {

  /**
   * the dimension to appear as horizontal dimension.
   */
  protected int dimx;

  /**
   * the dimension to appear as vertical dimension.
   */
  protected int dimy;

  /**
   * Setting up parameters individual to each run of the visualization.
   * 
   * @param dimx
   * @param dimy
   */
  public void setup(int dimx, int dimy) {
    this.dimx = dimx;
    this.dimy = dimy;
  }
}
