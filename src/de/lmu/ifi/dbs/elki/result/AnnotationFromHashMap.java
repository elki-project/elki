package de.lmu.ifi.dbs.elki.result;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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

  /**
   * Retrieve the annotations for the given ID.  
   */
  @Deprecated
  @Override
  @SuppressWarnings("deprecation")
  public Pair<String,T>[] getAnnotations(Integer objID) {
    Pair<String,T>[] result = Pair.newArray(1);
    T o = map.get(objID);
    result[0] = new Pair<String, T>(assoc.getLabel(), o);
    return result;
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
