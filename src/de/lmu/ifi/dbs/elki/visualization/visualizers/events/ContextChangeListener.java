package de.lmu.ifi.dbs.elki.visualization.visualizers.events;

import java.util.EventListener;

/**
 * Listener for context changes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent
 */
public interface ContextChangeListener extends EventListener {
  /**
   * Method called on context changes (e.g. projection changes).
   * Usually, this should trigger a redraw!
   * 
   * @param e Change event
   */
  public void contextChanged(ContextChangedEvent e);
}