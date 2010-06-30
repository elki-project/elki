package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event emitted when the active tool was changed.
 * 
 * @author Erich Schubert
 */
public class ActiveToolChangedEvent extends EventObject {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public ActiveToolChangedEvent(VisualizerContext<?> source) {
    super(source);
  }
}