package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import experimentalcode.remigius.visualization.ScalarVisualization;

public abstract class ScalarVisualizer<O extends DatabaseObject> extends AbstractVisualizer<O, ScalarVisualization> {
  
  protected int dim;
  
  public void init(int dim){
    this.dim = dim;
  }
}
