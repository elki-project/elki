package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event to signal when the current selection has changed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype event
 */
public class SelectionChangedEvent extends ContextChangedEvent {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   * 
   * @param source context that changed
   */
  public SelectionChangedEvent(VisualizerContext<?> source) {
    super(source);
  }
}
