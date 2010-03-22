package de.lmu.ifi.dbs.elki.utilities;

import java.util.HashMap;

/**
 * Associative storage based on a {@link HashMap} for multiple object types that
 * offers a type checked {@link #get(Object, Class)} method.
 * 
 * @author Erich Schubert
 * 
 * @param <K> Key class type
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
   * Type checked get method
   * 
   * @param <T> Return type
   * @param key Key
   * @param restriction restriction class
   * @return Object that is guaranteed to be of class restriction or null
   */
  public <T> T get(K key, Class<T> restriction) {
    Object o = super.get(key);
    if(o == null) {
      return null;
    }
    try {
      return restriction.cast(o);
    }
    catch(ClassCastException e) {
      return null;
    }
  }

  /**
   * (Largely) type checked get method for use with generic types
   * 
   * @param <T> Return type
   * @param key Key
   * @param restriction restriction class
   * @return Object that is guaranteed to be of class restriction or null
   */
  @SuppressWarnings("unchecked")
  public <T> T getGenerics(K key, Class<?> restriction) {
    return (T) get(key, restriction);
  }

  /**
   * Depreciate the use of the untyped get method.
   * 
   * @deprecated use {@link #get(Object, Class)} or
   *             {@link #getGenerics(Object, Class)} instead, for type safety!
   */
  @Override
  @Deprecated
  public Object get(Object key) {
    return super.get(key);
  }
}