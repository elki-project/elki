package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import experimentalcode.remigius.visualization.Visualization;

public abstract class DoubleVectorVisualizer<O extends DoubleVector, V extends Visualization> extends AbstractVisualizer<O, V> {
  
  protected LinearScale[] scales;
  
  public DoubleVectorVisualizer(){
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
