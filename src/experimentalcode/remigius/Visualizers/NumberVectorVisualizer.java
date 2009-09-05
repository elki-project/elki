package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import experimentalcode.remigius.VisualizationManager;

/**
 * Abstract superclass for Visualizers which process NumberVectors.
 * TODO: Refactor from DoubleVector to NumberVector
 * 
 * @author Remigius Wojdanowski
 *
 * @param <O> the type of object this visualizer will process.
 */
public abstract class NumberVectorVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends AbstractVisualizer<NV> {
  
  /**
   * Array of {@link LinearScale}-objects to calculate normalized positions of objects
   */
  protected LinearScale[] scales;
  
  /**
   * Convenience method, initializing the Visualizer with a default level of 0.
   * @see #init(Database, VisualizationManager, int, String)
   * 
   * @param db contains all objects to be processed.
   * @param v used to receive and publish different information.
   * @param name a short name characterizing this Visualizer
   */
  public void init(Database<NV> db, String name) {
    init(db, 0, name);
  }
  
  /**
   * Initializes the Visualizer, especially its scales.
   * @see AbstractVisualizer#init(Database, VisualizationManager, int, String)
   * 
   * @param db contains all objects to be processed.
   * @param v used to receive and publish different information.
   * @param name a short name characterizing this Visualizer
   */
  public void init(Database<NV> db, int level, String name) {
    super.init(db, 0, name);
    this.scales = Scales.calcScales(database);
  }
  
  /**
   * Returns a Double representing the position where the object will be placed.
   * 
   * @param o the object to be positioned.
   * @param dimx the dimension in which the position will be calculated
   * @return a Double representing the normalized position of the object in the
   *         given dimension.
   */
  public Double getPositioned(NV nv, int dim) {
    return getPositioned(nv.getValue(dim).doubleValue(), dim);
  }
  
  public Double getPositioned(Number n, int dim) {
    return scales[dim].getScaled(n.doubleValue());
  }
}
