package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisFactory;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has Projection2DVisualization oneway - - produces
 * @apiviz.has ProjectedThumbnail
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public abstract class P2DVisFactory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> implements ProjectedVisFactory<NV> {
  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   * @param level the visualizer level 
   */
  public P2DVisFactory(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected P2DVisFactory(String name) {
    super(name);
  }
}