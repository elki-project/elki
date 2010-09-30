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
public interface Result extends AnyResult {
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
}