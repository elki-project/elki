package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.visualization.PlanarVisualization;

// TODO: Replace DoubleVector with NumberVector? 
public abstract class PlanarVisualizer<O extends DoubleVector> extends AbstractVisualizer<O, PlanarVisualization> {

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
  public Double getPositioned(O o, int dimx) {
    return scales[dimx].getScaled(o.getValue(dimx));
  }
  
  @Override
  protected abstract PlanarVisualization visualize(SVGPlot svgp, Element layer);
}
