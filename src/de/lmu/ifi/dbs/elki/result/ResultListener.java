package de.lmu.ifi.dbs.elki.result;

import java.util.EventListener;

/**
 * Listener interface invoked when new results are added to the result tree.
 * 
 * @author Erich Schubert
 */
public interface ResultListener extends EventListener {
  /**
   * A new derived result was added.
   * 
   * @param r New child result added
   * @param parent Parent result that was added to
   */
  public void resultAdded(AnyResult r, Result parent);
  
  /**
   * A result was removed.
   * 
   * @param r result that was removed
   * @param parent Parent result that was removed from
   */
  public void resultRemoved(AnyResult r, Result parent);
}
