package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import experimentalcode.remigius.VisualizationManager;

/**
 * Abstract superclass for Visualizers which process NumberVectors.
 * 
 * @author Remigius Wojdanowski
 * 
 * TODO: Add missing documentation for the parameters. This todo also includes (all) sub-classes.
 * 
 * @param <NV>
 * @param <N>
 */
public abstract class NumberVectorVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends AbstractVisualizer<NV> {

  /**
   * Array of {@link LinearScale}-objects to calculate normalized positions of
   * objects.
   */
  protected LinearScale[] scales;

  /**
   * Convenience method, initializing this Visualizer with a default level of 0.
   * 
   * @see #init(Database, VisualizationManager, int, String)
   * 
   * @param db contains all objects to be processed.
   * @param v used to receive and publish different information.
   * @param name a short name characterizing this Visualizer.
   */
  public void init(Database<NV> db, String name) {
    init(db, 0, name);
  }

  /**
   * Initializes this Visualizer, especially its scales. <br>
   * This method acts as a replacement for the constructor, which can't take any
   * arguments due to restrictions imposed by the way parameters are collected.
   * 
   * @see AbstractVisualizer#init(Database, VisualizationManager, int, String)
   * 
   * @param db contains all objects to be processed.
   * @param v used to receive and publish different information.
   */
  public void init(Database<NV> db, int level, String name) {
    super.init(db, level, name);
    this.scales = Scales.calcScales(database);
  }

  /**
   * Returns a Double representing the position where the object will be placed.
   * 
   * @see #getPositioned(NumberVector, int)
   * 
   * @param o the object to be positioned.
   * @param dim the dimension in which the position will be calculated.
   * @return a Double representing the scaled position of the object in the
   *         given dimension.
   */
  public Double getPositioned(NV nv, int dim) {
    return getPositioned(nv.getValue(dim).doubleValue(), dim);
  }

  /**
   * Returns a Double representing a given coordinate being scaled
   * 
   * @param n the coordinate to scale.
   * @param dim the dimension indicating the scale to be used.
   * @return a Double representing a given coordinate being scaled appropriately
   *         to our actual coordinate system.
   */
  public Double getPositioned(Number n, int dim) {
    return scales[dim].getScaled(n.doubleValue());
  }
}
