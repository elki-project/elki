package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisFactory;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.has ProjectedThumbnail
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public abstract class P1DVisFactory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> implements ProjectedVisFactory<NV> {
  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   * @param level the visualizer level 
   */
  public P1DVisFactory(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected P1DVisFactory(String name) {
    super(name);
  }
}