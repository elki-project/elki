package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public abstract class ScalarVisualizer<O extends DoubleVector> extends DoubleVectorVisualizer<O> {
  
  protected int dim;
  
  public void setup(int dim){
    this.dim = dim;
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
  
  @Override
  public abstract Element visualize(SVGPlot svgp);
}
