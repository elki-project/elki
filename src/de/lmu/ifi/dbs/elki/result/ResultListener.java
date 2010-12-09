package de.lmu.ifi.dbs.elki.result;

import java.util.EventListener;

/**
 * Listener interface invoked when new results are added to the result tree.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Result
 */
public interface ResultListener extends EventListener {
  /**
   * A new derived result was added.
   * 
   * @param child New child result added
   * @param parent Parent result that was added to
   */
  public void resultAdded(Result child, Result parent);
  
  /**
   * Notify that the current result has changed substantially.
   * 
   * @param current Result that has changed.
   */
  public void resultChanged(Result current);
  
  /**
   * A result was removed.
   * 
   * @param child result that was removed
   * @param parent Parent result that was removed from
   */
  public void resultRemoved(Result child, Result parent);
}
