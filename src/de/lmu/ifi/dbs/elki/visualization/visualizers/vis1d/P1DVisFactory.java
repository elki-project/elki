package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses P1DVisualization oneway - - «create»
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public abstract class P1DVisFactory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
  /**
   * Constructor.
   */
  protected P1DVisFactory() {
    super();
  }
}