package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Annotations backed by a DataStore.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type to store.
 */
// TODO: make serializable.
public class AnnotationFromDataStore<T> extends TreeResult implements AnnotationResult<T> {
  /**
   * Store the hashmap for results.
   */
  private DataStore<? extends T> map;
  
  /**
   * Store Association ID
   */
  private AssociationID<T> assoc;

  /**
   * Constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param assoc Association
   * @param map Map
   */
  public AnnotationFromDataStore(String name, String shortname, AssociationID<T> assoc, DataStore<? extends T> map) {
    super(name, shortname);
    this.map = map;
    this.assoc = assoc;
  }

  @Override
  public AssociationID<T> getAssociationID() {
    return assoc;
  }

  @Override
  public T getValueFor(DBID objID) {
    return map.get(objID);
  }
}