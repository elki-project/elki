package de.lmu.ifi.dbs.elki.utilities.designpattern;

/**
 * Simple Observer design pattern.
 * 
 * @author Erich Schubert
 * 
 * @param T object type to observe
 */
public interface Observer<T> {
  /**
   * This method is called when an observed object was updated.
   * 
   * @param o Observable
   */
  public void update(T o);
}