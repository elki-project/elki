package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
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
public class AnnotationsFromHashMap<T> implements AnnotationResult<T> {
  /**
   * Store the hashmaps for results.
   */
  private ArrayList<Pair<String,HashMap<Integer,T>>> maps = new ArrayList<Pair<String,HashMap<Integer,T>>>();

  /**
   * Constructor
   */
  public AnnotationsFromHashMap() {
    super();
  }

  /**
   * Add a single hashmap to the annotations 
   * 
   * @param assoc Association ID
   * @param map Hashmap to back the result.
   */
  public void addMap(AssociationID<?> assoc, HashMap<Integer,T> map) {
    maps.add(new Pair<String,HashMap<Integer,T>>(assoc.getLabel(), map));
  }

  /**
   * Add a single hashmap to the annotations 
   * 
   * @param name Annotation label
   * @param map Hashmap to back the result.
   */
  public void addMap(String name, HashMap<Integer,T> map) {
    maps.add(new Pair<String,HashMap<Integer,T>>(name, map));
  }

  /**
   * Retrieve the annotations for the given ID.  
   */
  @Override
  public Pair<String,T>[] getAnnotations(Integer objID) {
    Pair<String,T>[] result = Pair.newArray(maps.size());
    int index = 0;
    for (Pair<String, HashMap<Integer,T>> pair : maps) {
      // TODO: null handling? skip null values?
      T o = pair.getSecond().get(objID);
      result[index] = new Pair<String, T>(pair.getFirst(), o);
      index++;
    }
    return result;
  }
}
