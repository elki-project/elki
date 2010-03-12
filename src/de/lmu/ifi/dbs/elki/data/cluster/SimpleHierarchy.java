package de.lmu.ifi.dbs.elki.data.cluster;

import java.util.Collection;
import java.util.List;

/**
 * Hierarchy implementation with a per-object representation.
 * 
 * @author Erich Schubert
 * 
 * @param <C>
 */
// TODO: Hierarchy implementation with central storage (and parent-child
// symmetry guarantee)
public class SimpleHierarchy<C extends HierarchyInterface<C>> implements HierarchyImplementation<C> {
  /**
   * Owner
   */
  private C owner;

  /**
   * Storage for children
   */
  private List<C> children;

  /**
   * Storage for parents
   */
  private List<C> parents;

  /**
   * Constructor for hierarchy object.
   * 
   * @param owner owning cluster.
   * @param children child clusters. May be null.
   * @param parents parent clusters. May be null.
   */
  public SimpleHierarchy(C owner, List<C> children, List<C> parents) {
    super();
    this.owner = owner;
    this.children = children;
    this.parents = parents;
  }

  /**
   * Return that this model is hierarchical.
   */
  public final boolean isHierarchical() {
    return true;
  }

  /**
   * Return number of children.
   */
  @Override
  public int numChildren(C self) {
    if(owner != self) {
      return -1;
    }
    return children.size();
  }

  /**
   * Return children
   */
  @Override
  public List<C> getChildren(C self) {
    if(owner != self) {
      return null;
    }
    return children;
  }

  /**
   * Collect descendants.
   */
  @Override
  public <T extends Collection<C>> T getDescendants(C self, T collection) {
    if(owner != self) {
      return collection;
    }
    for(C child : children) {
      if(!collection.contains(child)) {
        collection.add(child);
        child.getDescendants(collection);
      }
    }
    return collection;
  }

  /**
   * Return number of parents
   */
  @Override
  public int numParents(C self) {
    if(owner != self) {
      return -1;
    }
    return parents.size();
  }

  /**
   * Return parents
   */
  @Override
  public List<C> getParents(C self) {
    if(owner != self) {
      return null;
    }
    return parents;
  }

  /**
   * Collect ancestors.
   */
  @Override
  public <T extends Collection<C>> T getAncestors(C self, T collection) {
    if(owner != self) {
      return collection;
    }
    for(C parent : parents) {
      if(!collection.contains(parent)) {
        collection.add(parent);
        parent.getAncestors(collection);
      }
    }
    return collection;
  }
}