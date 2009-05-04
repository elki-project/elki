package de.lmu.ifi.dbs.elki.result;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.database.AssociationID;

/**
 * Annotations backed by hashmaps.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type to store.
 */
// TODO: make serializable.
public class AnnotationFromHashMap<T> implements AnnotationResult<T> {
  /**
   * Store the hashmap for results.
   */
  private HashMap<Integer,T> map;
  
  /**
   * Store Association ID
   */
  private AssociationID<T> assoc;

  /**
   * Constructor
   * @param assoc Association
   * @param map Map
   */
  public AnnotationFromHashMap(AssociationID<T> assoc, HashMap<Integer,T> map) {
    this.map = map;
    this.assoc = assoc;
  }

  @Override
  public AssociationID<T> getAssociationID() {
    return assoc;
  }

  @Override
  public T getValueFor(Integer objID) {
    return map.get(objID);
  }
}
