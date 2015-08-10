package de.lmu.ifi.dbs.elki.visualization.projector;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProcessor;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * A projector is responsible for adding projections to the visualization by
 * detecting appropriate relations in the database.
 *
 * @author Erich Schubert
 *
 * @apiviz.has Projector
 */
public interface ProjectorFactory extends VisualizationProcessor {
  /**
   * Add projections for the given result (tree) to the result tree.
   * @param context Visualization context
   * @param start Result to process
   */
  @Override
  public void processNewResult(VisualizerContext context, Object start);
}