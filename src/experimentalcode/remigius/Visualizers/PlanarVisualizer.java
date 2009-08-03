package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;

// TODO: Replace DoubleVector with NumberVector? - No, as long as we need DoubleVector for calcScales. 
public abstract class PlanarVisualizer<O extends DoubleVector> extends AbstractVisualizer<O> {

  protected int dimx;
  protected int dimy;
  
  protected LinearScale[] scales;
  
  public void init(int dimx, int dimy){
    this.dimx = dimx;
    this.dimy = dimy;
    this.scales = Scales.calcScales(database);
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
