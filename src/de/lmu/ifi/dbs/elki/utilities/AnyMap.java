package de.lmu.ifi.dbs.elki.utilities;

import java.util.HashMap;

/**
 * Associative storage based on a {@link HashMap} for multiple object
 * types that offers a type checked {@link #get(Object, Class)} method.
 * 
 * The use of the inherited {@link #get(Object)} method is depreciated.
 * 
 * @author Erich Schubert
 */
public class AnyMap<K> extends HashMap<K, Object> {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Constructor
   */
  public AnyMap() {
    super();
  }
  
  /**
   * (Largely) Type checked get method
   * 
   * @param <T> Return type
   * @param key Key
   * @param restriction restriction class
   * @return Object that is guaranteed to be of class restriction or null
   */
  @SuppressWarnings("unchecked")
  public <T> T get(K key, Class<?> restriction) {
    Object o = super.get(key);
    if (o == null) {
      return null;
    }
    try {
      T r = (T) restriction.cast(o);
      return r;
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Depreciate the use of the untyped get method.
   */
  @Override
  @Deprecated
  public Object get(Object key) {
    return super.get(key);
  }
}