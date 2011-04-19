package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.EventListener;

/**
 * Defines the interface for an object that listens to changes in a
 * {@link DataStore}.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses DataStoreEvent oneway - - listens
 * @apiviz.excludeSubtypes
 * 
 * @see DataStore
 * @see DataStoreEvent
 * @param <T> the data type as element of the {@link DataStore}
 */
public interface DataStoreListener extends EventListener {
  /**
   * Invoked after objects of the datastore have been updated, inserted or
   * removed in some way. 
   * 
   * @param e the update event
   */
  void contentChanged(DataStoreEvent e);

  /**
   * Invoked after the data store has been destroyed.
   * 
   * @param e the destroy event
   */
  //void dataStoreDestroyed(DataStoreEvent e);
}
