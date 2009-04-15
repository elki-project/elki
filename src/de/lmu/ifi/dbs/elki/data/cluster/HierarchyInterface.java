package de.lmu.ifi.dbs.elki.data.cluster;

import java.util.Collection;
import java.util.List;

/**
 * Interface to access hierarchies.
 * 
 * @author Erich Schubert
 *
 * @param <C> Object type in hierarchy
 */
public interface HierarchyInterface<C> {
  /**
   * Test for hierarchical properties
   * 
   * @return hierarchical data model.
   */
  public boolean isHierarchical();

  /**
   * Get number of children
   * @return number of children
   */
  public int numChildren();
  /**
   * Get children list.
   * Resulting list MAY be modified.
   * Result MAY be null, if the model is not hierarchical.
   * 
   * @return list of children
   */
  public List<C> getChildren();
  /**
   * Collect descendants (recursive children)
   * 
   * @param <T> collection type
   * @param collection Collection to fill
   * @return filled collection 
   */
  public <T extends Collection<C>> T getDescendants(T collection);
  /**
   * Get number of parents
   * @return number of parents
   */
  public int numParents();
  /**
   * Get parents list.
   * Resulting list MAY be modified.
   * Result MAY be null, if the model is not hierarchical.
   * 
   * @return list of parents
   */
  public List<C> getParents();
  /**
   * Collect ancestors (recursive parents)
   * 
   * @param <T> collection type
   * @param collection Collection to fill.
   * @return filled collection
   */
  public <T extends Collection<C>> T getAncestors(T collection);
}
