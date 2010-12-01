package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event emitted when a visualizer was enabled or disabled (including tool
 * changes!)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype event
 * @apiviz.has Visualizer
 */
public class VisualizerChangedEvent extends ContextChangedEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualizer that was changed
   */
  private VisFactory vis;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public VisualizerChangedEvent(VisualizerContext<?> source, VisFactory vis) {
    super(source);
    this.vis = vis;
  }

  /**
   * Visualizer which was changed.
   * 
   * @return the visualizer affected
   */
  public VisFactory getVisualizer() {
    return vis;
  }
}