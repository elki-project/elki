package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

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
  private ArrayList<SimplePair<String,HashMap<Integer,T>>> maps = new ArrayList<SimplePair<String,HashMap<Integer,T>>>();

  /**
   * Constructor
   */
  public AnnotationsFromHashMap() {
  }

  /**
   * Add a single hashmap to the annotations 
   * 
   * @param name Annotation label
   * @param map Hashmap to back the result.
   */
  public void addMap(String name, HashMap<Integer,T> map) {
    maps.add(new SimplePair<String,HashMap<Integer,T>>(name, map));
  }

  /**
   * Retrieve the annotations for the given ID.  
   */
  @Override
  public SimplePair<String,T>[] getAnnotations(Integer objID) {
    SimplePair<String,T>[] result = SimplePair.newArray(maps.size());
    int index = 0;
    for (SimplePair<String, HashMap<Integer,T>> pair : maps) {
      // TODO: null handling? skip null values?
      T o = pair.getSecond().get(objID);
      result[index] = new SimplePair<String, T>(pair.getFirst(), o);
      index++;
    }
    return result;
  }
}
