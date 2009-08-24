package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public abstract class ScalarVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends DoubleVectorVisualizer<NV, N> {
  
  protected int dim;
  
  public void setup(int dim){
    this.dim = dim;
  }
  
  @Override
  public abstract Element visualize(SVGPlot svgp);
}
