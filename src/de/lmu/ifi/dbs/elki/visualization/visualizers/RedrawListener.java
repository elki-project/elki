package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.EventListener;

/**
 * Listener for Redraw events.
 * 
 * @author Erich Schubert
 */
public interface RedrawListener extends EventListener {
  /**
   * Called when a redraw is needed.
   */
  public void triggerRedraw();
}
