package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Event triggered when the contexts view was resized.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype event
 */
public class ResizedEvent extends ContextChangedEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   * 
   * @param source Visualization context
   */
  public ResizedEvent(VisualizerContext source) {
    super(source);
  }
}