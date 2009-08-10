package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Produces 2-dimensional visualizations.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <O> The type of object to process.
 */
// TODO: Replace DoubleVector with NumberVector? - No, as long as we need DoubleVector for calcScales. 
public abstract class PlanarVisualizer<O extends DoubleVector> extends DoubleVectorVisualizer<O> {
  
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
  public void setup(int dimx, int dimy){
    this.dimx = dimx;
    this.dimy = dimy;
  }
  
  /**
   * Returns a Double representing a position where the object will be placed
   * 
   * @param o the object to be positioned.
   * @param dimx the dimension in which the position will be calculated
   * @return a Double representing the normalized position of the object in the
   *         given dimension.
   */
  public Double getPositioned(O o, int dim) {
    return scales[dim].getScaled(o.getValue(dim));
  }
}
