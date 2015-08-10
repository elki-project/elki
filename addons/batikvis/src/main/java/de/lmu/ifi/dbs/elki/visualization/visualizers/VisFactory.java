package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProcessor;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Defines the requirements for a visualizer. <br>
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor) to be used for parameterization.
 *
 * @author Remigius Wojdanowski
 *
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses Visualization - - «create»
 * @apiviz.uses VisualizationTask - - «create»
 */
public interface VisFactory extends VisualizationProcessor {
  /**
   * Add visualizers for the given result (tree) to the context.
   * @param context TODO
   * @param context Visualization context
   * @param start Result to process
   */
  @Override
  public void processNewResult(VisualizerContext context, Object start);

  /**
   * Produce a visualization instance for the given task
   *
   * @param task Visualization task
   * @return Visualization
   */
  public Visualization makeVisualization(VisualizationTask task);

  /**
   * Produce a visualization instance for the given task that may use thumbnails
   *
   * @param task Visualization task
   * @return Visualization
   */
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task);
}