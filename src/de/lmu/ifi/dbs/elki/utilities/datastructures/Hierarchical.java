package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.List;


/**
 * Interface for objects with an <b>internal</b> hierarchy interface.
 * 
 * Note that the object can chose to delegate the hierarchy to an external hierarchy.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type in hierarchy
 */
public interface Hierarchical<O> {
  /**
   * Test for hierarchical properties
   * 
   * @return hierarchical data model.
   */
  public boolean isHierarchical();

  /**
   * Get number of children
   * 
   * @return number of children
   */
  public int numChildren();

  /**
   * Get children list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @return list of children
   */
  public List<O> getChildren();

  /**
   * Iterate descendants (recursive children)
   * 
   * @return iterator for descendants
   */
  public IterableIterator<O> iterDescendants();
  
  /**
   * Get number of parents
   * 
   * @return number of parents
   */
  public int numParents();

  /**
   * Get parents list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @return list of parents
   */
  public List<O> getParents();

  /**
   * Iterate ancestors (recursive parents)
   * 
   * @return iterator for ancestors
   */
  public IterableIterator<O> iterAncestors();
}