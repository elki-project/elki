package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event produced when the visualizer context has changed.
 * 
 * @author Erich Schubert
 */
public abstract class ContextChangedEvent extends EventObject {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Visualization context changed.
   * 
   * @param source context that has changed
   */
  public ContextChangedEvent(VisualizerContext<?> source) {
    super(source);
  }
}