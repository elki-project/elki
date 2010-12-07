package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;

/**
 * This interface represents an (external) hierarchy of objects. It can contain
 * arbitrary objects, BUT the hierarchy has to be accessed using the hierarchy
 * object, i.e. {@code hierarchy.getChildren(object);}.
 * 
 * See {@link Hierarchical} for an interface for objects with an internal
 * hierarchy (where you can use {@code object.getChildren();})
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface Hierarchy<O> {
  /**
   * Get number of children
   * 
   * @param self object to get number of children for
   * @return number of children
   */
  public int numChildren(O self);

  /**
   * Get children list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @param self object to get children for
   * @return list of children
   */
  public List<O> getChildren(O self);

  /**
   * Iterate descendants (recursive children)
   * 
   * @param self object to get descendants for
   * @return iterator for descendants
   */
  public IterableIterator<O> iterDescendants(O self);

  /**
   * Get number of (direct) parents
   * 
   * @param self reference object
   * @return number of parents
   */
  public int numParents(O self);

  /**
   * Get parents list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @param self object to get parents for
   * @return list of parents
   */
  public List<O> getParents(O self);

  /**
   * Iterate ancestors (recursive parents)
   * 
   * @param self object to get ancestors for
   * @return iterator for ancestors
   */
  public IterableIterator<O> iterAncestors(O self);
}