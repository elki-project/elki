package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.EmptyIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;

/**
 * Centralized hierarchy implementation, using a HashMap of Lists.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type (arbitrary!)
 */
public class HierarchyHashmapList<O> implements ModifiableHierarchy<O> {
  /**
   * The data storage for parents
   */
  final private HashMap<O, List<O>> pmap;

  /**
   * The data storage for children
   */
  final private HashMap<O, List<O>> cmap;

  /**
   * Constructor
   */
  public HierarchyHashmapList() {
    super();
    this.pmap = new HashMap<O, List<O>>();
    this.cmap = new HashMap<O, List<O>>();
  }

  @Override
  public void add(O parent, O child) {
    // Add child to parent.
    {
      List<O> pchi = this.cmap.get(parent);
      if(pchi == null) {
        pchi = new LinkedList<O>();
        this.cmap.put(parent, pchi);
      }
      if(!pchi.contains(child)) {
        pchi.add(child);
      } else {
        LoggingUtil.warning("Result added twice: "+parent+" -> "+child);
      }
    }
    // Add child to parent
    {
      List<O> cpar = this.pmap.get(child);
      if(cpar == null) {
        cpar = new LinkedList<O>();
        this.pmap.put(child, cpar);
      }
      if(!cpar.contains(parent)) {
        cpar.add(parent);
      } else {
        LoggingUtil.warning("Result added twice: "+parent+" <- "+child);
      }
    }
  }

  @Override
  public void remove(O parent, O child) {
    // Add child to parent.
    {
      List<O> pchi = this.cmap.get(parent);
      if(pchi != null) {
        while(pchi.remove(child)) {
          // repeat - remove all instances
        }
        if(pchi.size() == 0) {
          this.cmap.remove(parent);
        }
      }
    }
    // Add child to parent
    {
      List<O> cpar = this.pmap.get(child);
      if(cpar != null) {
        while(cpar.remove(parent)) {
          // repeat - remove all instances
        }
        if(cpar.size() == 0) {
          this.pmap.remove(child);
        }
      }
    }
  }

  /**
   * Put an object along with parent and child lists.
   * 
   * @param obj Object
   * @param parents Parent list
   * @param children Child list
   */
  public void put(O obj, List<O> parents, List<O> children) {
    this.pmap.put(obj, parents);
    this.cmap.put(obj, children);
  }

  @Override
  public int numChildren(O obj) {
    List<O> children = this.cmap.get(obj);
    if(children == null) {
      return 0;
    }
    return children.size();
  }

  @Override
  public List<O> getChildren(O obj) {
    List<O> children = this.cmap.get(obj);
    if(children == null) {
      return Collections.emptyList();
    }
    return children;
  }

  @Override
  public IterableIterator<O> iterDescendants(O obj) {
    return new ItrDesc(obj);
  }

  @Override
  public int numParents(O obj) {
    List<O> parents = this.pmap.get(obj);
    if(parents == null) {
      return 0;
    }
    return parents.size();
  }

  @Override
  public List<O> getParents(O obj) {
    List<O> parents = this.pmap.get(obj);
    if(parents == null) {
      return Collections.emptyList();
    }
    return parents;
  }

  @Override
  public IterableIterator<O> iterAncestors(O obj) {
    return new ItrAnc(obj);
  }

  /**
   * Iterator to collect into the descendants.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrDesc implements IterableIterator<O> {
    /**
     * Starting object (for cloning);
     */
    final O start;

    /**
     * Iterator over children
     */
    final Iterator<O> childiter;

    /**
     * Iterator of current child
     */
    Iterator<O> subiter;

    public ItrDesc(O start) {
      this.start = start;
      List<O> children = getChildren(start);
      if(children != null) {
        this.childiter = children.iterator();
      }
      else {
        this.childiter = EmptyIterator.STATIC();
      }
      this.subiter = null;
    }

    @Override
    public boolean hasNext() {
      if(subiter != null && subiter.hasNext()) {
        return true;
      }
      return childiter.hasNext();
    }

    @Override
    public O next() {
      // Try nested iterator first ...
      if(subiter != null && subiter.hasNext()) {
        return subiter.next();
      }
      // Next direct child, update subiter.
      final O child = childiter.next();
      subiter = iterDescendants(child);
      return child;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<O> iterator() {
      return new ItrDesc(start);
    }
  }

  /**
   * Iterator over all Ancestors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrAnc implements IterableIterator<O> {
    /**
     * Starting object (for cloning);
     */
    final O start;

    /**
     * Iterator over parents
     */
    final Iterator<O> parentiter;

    /**
     * Iterator of current parent
     */
    Iterator<O> subiter;

    public ItrAnc(O start) {
      this.start = start;
      List<O> parents = getParents(start);
      if(parents != null) {
        this.parentiter = parents.iterator();
      }
      else {
        this.parentiter = EmptyIterator.STATIC();
      }
      this.subiter = null;
    }

    @Override
    public boolean hasNext() {
      if(subiter != null && subiter.hasNext()) {
        return true;
      }
      return parentiter.hasNext();
    }

    @Override
    public O next() {
      // Try nested iterator first ...
      if(subiter != null && subiter.hasNext()) {
        return subiter.next();
      }
      // Next direct parent, update subiter.
      final O parent = parentiter.next();
      subiter = iterAncestors(parent);
      return parent;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<O> iterator() {
      return new ItrAnc(start);
    }
  }
}