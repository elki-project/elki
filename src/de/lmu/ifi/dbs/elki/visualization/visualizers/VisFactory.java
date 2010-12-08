package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

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
 * 
 * @param <O> object type
 */
public interface VisFactory<O extends DatabaseObject> extends Parameterizable {
  /**
   * Add visualizers for the given result (tree) to the context.
   * 
   * @param context Context to work with
   * @param result Result to process
   */
  public void addVisualizers(VisualizerContext<? extends O> context, Result result);

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