package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;


/**
 * Modifiable Hierarchy.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface ModifiableHierarchy<O> extends Hierarchy<O> {
  /**
   * Add a parent-child relationship.
   * 
   * @param parent Parent
   * @param child Child
   */
  // TODO: return true when new?
  public void add(O parent, O child);

  /**
   * Remove a parent-child relationship.
   * 
   * @param parent Parent
   * @param child Child
   */
  // TODO: return true when found?
  public void remove(O parent, O child);
}
