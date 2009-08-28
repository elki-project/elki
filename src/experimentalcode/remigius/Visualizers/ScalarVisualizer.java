package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;

public abstract class ScalarVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends NumberVectorVisualizer<NV, N> {
  
  protected int dim;
  
  public void setup(int dim){
    this.dim = dim;
  }
}
