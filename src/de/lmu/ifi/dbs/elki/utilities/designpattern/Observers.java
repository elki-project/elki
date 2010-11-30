package de.lmu.ifi.dbs.elki.utilities.designpattern;

/**
 * Class to manage the observers of an instance.
 * 
 * Design note: to avoid reference cycles, this object does not keep track of its owner.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype delegate
 * @apiviz.has Observer
 */
public class Observers<T> extends java.util.Vector<Observer<? super T>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Constructor.
   */
  public Observers() {
    super();
  }
  
  /**
   * Add an observer to the object.
   * 
   * @param o Observer to add
   */
  public void addObserver(Observer<? super T> o) {
    super.add(o);
  }

  /**
   * Remove an observer from the object.
   * 
   * @param o Observer to remove
   */
  public void removeObserver(Observer<? super T> o) {
    super.remove(o);
  }
  
  /**
   * Notify the observers of the changed object.
   * 
   * @param owner Owner of the Observers list - changed instance
   */
  public void notifyObservers(T owner) {
    for (Observer<? super T> observer : this) {
      observer.update(owner);
    }
  }
}