package de.lmu.ifi.dbs.elki.data.cluster;

import java.util.Collection;
import java.util.List;

/**
 * Hierarchy abstraction layer.
 * Note that this allows the hierarchy to be implemented either using one
 * Hierarchy Object per C object, or using a shared object for all of them.
 * "self" is the parameter that is the referencing object.
 * 
 * @author Erich Schubert
 *
 * @param <C> Cluster type
 */
public interface HierarchyImplementation<C extends HierarchyInterface<C>> {
  /**
   * Test for hierarchical properties
   * 
   * @return hierarchical data model.
   */
  public boolean isHierarchical();
  
  /**
   * Get number of children
   * @param self object to get number of children for
   * @return number of children
   */
  public int numChildren(C self);
  
  /**
   * Get children list.
   * Resulting list MAY be modified.
   * Result MAY be null, if the model is not hierarchical.
   * 
   * @param self object to get children for
   * @return list of children
   */
  public List<C> getChildren(C self);
  
  /**
   * Collect descendants (recursive children)
   * 
   * @param <T> Collection type
   * @param self object to get descendants for
   * @param collection Collection to fill
   * @return collection of descendants
   */
  public <T extends Collection<C>> T getDescendants(C self, T collection);
  
  /**
   * Get number of parents
   * @param self reference object
   * @return number of parents
   */
  public int numParents(C self);
  
  /**
   * Get parents list.
   * Resulting list MAY be modified.
   * Result MAY be null, if the model is not hierarchical.
   * 
   * @param self object to get parents for
   * @return list of parents
   */
  public List<C> getParents(C self);
  
  /**
   * Collect ancestors (recursive parents)
   * 
   * @param <T> Collection type
   * @param self object to get anchestors for
   * @param collection Collection to fill
   * @return collection of ancestors
   */
  public <T extends Collection<C>> T getAncestors(C self, T collection);
}