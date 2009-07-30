package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.visualization.PlanarVisualization;

// TODO: Replace DoubleVector with NumberVector? - No, as long as we need DoubleVector for calcScales. 
public abstract class PlanarVisualizer<O extends DoubleVector> extends DoubleVectorVisualizer<O, PlanarVisualization> {

  protected int dimx;
  protected int dimy;
  
  public void init(int dimx, int dimy){
    this.dimx = dimx;
    this.dimy = dimy;
  }
  
  @Override
  protected abstract PlanarVisualization visualize(SVGPlot svgp, Element layer);
}
