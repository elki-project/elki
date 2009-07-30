package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import experimentalcode.remigius.visualization.ScalarVisualization;

public abstract class ScalarVisualizer<O extends DoubleVector> extends DoubleVectorVisualizer<O, ScalarVisualization> {
  
  protected int dim;
  
  public void init(int dim){
    this.dim = dim;
  }
}
