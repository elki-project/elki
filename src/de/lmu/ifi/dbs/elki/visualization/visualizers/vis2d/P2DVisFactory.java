package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisFactory;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses P2DVisualization oneway - - «create»
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public abstract class P2DVisFactory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> implements ProjectedVisFactory<NV> {
  /**
   * Constructor.
   */
  protected P2DVisFactory() {
    super();
  }
}