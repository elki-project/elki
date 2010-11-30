package de.lmu.ifi.dbs.elki.utilities.designpattern;

/**
 * Observable design pattern.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Observers
 * 
 * @param <T> the object to observer
 */
public interface Observable<T> {
  /**
   * Add an observer to the object.
   * 
   * @param o Observer to add
   */
  public void addObserver(Observer<? super T> o);
  /**
   * Remove an observer from the object.
   * 
   * @param o Observer to remove
   */
  public void removeObserver(Observer<? super T> o);
}