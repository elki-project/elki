package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * One-dimensional projected visualization.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Projection1D
 */
public abstract class P1DVisualization<NV extends NumberVector<NV, ?>> extends AbstractVisualization<NV> {
  /**
   * The current projection
   */
  final protected Projection1D proj;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public P1DVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
  }
}