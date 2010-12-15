package de.lmu.ifi.dbs.elki.index.preprocessed;

import java.util.EventListener;

/**
 * Listener interface invoked when the k nearest neighbors (kNNs) of some
 * objects have been changed due to insertion or removals of objects.
 * 
 * @author Elke Achtert
 */
public interface KNNListener extends EventListener {
  /**
   * Invoked after kNNs have been updated, inserted or removed
   * in some way.
   * 
   * @param e the change event
   */
  public void kNNsChanged(KNNChangeEvent e);

  /**
   * Existing objects have been removed and as a result existing kNNs have been
   * removed and some kNNs have been changed.
   * 
   * @param source the object responsible for the invocation
   * @param removals the ids of the removed kNNs
   * @param updates the ids of kNNs which have been changed due to the removals
   */
  // public void kNNsRemoved(Object source, DBIDs removals, DBIDs updates);
}
