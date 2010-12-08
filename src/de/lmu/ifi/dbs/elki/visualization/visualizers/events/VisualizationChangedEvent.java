package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event emitted when a visualizer was enabled or disabled (including tool
 * changes!)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype event
 * @apiviz.has VisualizationTask
 */
public class VisualizationChangedEvent extends ContextChangedEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualizer that was changed
   */
  private VisualizationTask vis;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public VisualizationChangedEvent(VisualizerContext<?> source, VisualizationTask vis) {
    super(source);
    this.vis = vis;
  }

  /**
   * Visualizer which was changed.
   * 
   * @return the visualizer affected
   */
  public VisualizationTask getVisualizer() {
    return vis;
  }
}