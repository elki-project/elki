package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

/**
 * Interface for "full" result objects, that allow annotation and nesting.
 * 
 * The general concept is that a result is dependent on and only on its
 * ancestors primary results. Additional derived results can be inserted.
 * 
 * @author Erich Schubert
 */
public interface Result extends AnyResult, ResultListener {
  /**
   * Primary results represented
   * 
   * @return the primary results
   */
  public Collection<AnyResult> getPrimary();

  /**
   * Derived results represented
   * 
   * @return the derived results
   */
  public Collection<AnyResult> getDerived();
  
  /**
   * Add a new derived result
   * 
   * @param r new result
   */
  public void addDerivedResult(AnyResult r);

  /**
   * Add a listener to be notified on new results.
   * 
   * @param l Listener to add
   */
  public void addResultListener(ResultListener l);

  /**
   * Remove a listener to be notified on new results.
   * 
   * @param l Listener to remove
   */
  public void removeResultListener(ResultListener l);
}