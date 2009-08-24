package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public abstract class ScalarVisualizer<O extends DoubleVector> extends DoubleVectorVisualizer<O> {
  
  protected int dim;
  
  public void setup(int dim){
    this.dim = dim;
  }
  
  @Override
  public abstract Element visualize(SVGPlot svgp);
}
