package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event to signal when the current projection has changed.
 * 
 * @author Erich Schubert
 */
public class ProjectionChangedEvent extends ContextChangedEvent {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   * 
   * @param source context that changed
   */
  public ProjectionChangedEvent(VisualizerContext<?> source) {
    super(source);
  }
}
