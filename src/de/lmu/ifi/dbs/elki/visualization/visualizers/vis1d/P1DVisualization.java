package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * One-dimensional projected visualization.
 * 
 * @author Erich Schubert
 */
public abstract class P1DVisualization<NV extends NumberVector<NV, ?>> extends AbstractVisualization<NV> {
  /**
   * The current projection
   */
  final protected Projection1D proj;

  /**
   * Constructor.
   * 
   * @param context Context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   * @param level Level
   */
  public P1DVisualization(VisualizationTask task, Integer level) {
    super(task, level);
    this.proj = task.getProj();
  }
}