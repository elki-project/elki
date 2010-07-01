package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event emitted when a visualizer was enabled or disabled (including tool changes!)
 * 
 * @author Erich Schubert
 */
public class VisualizerChangedEvent extends ContextChangedEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public VisualizerChangedEvent(VisualizerContext<?> source) {
    super(source);
  }
}