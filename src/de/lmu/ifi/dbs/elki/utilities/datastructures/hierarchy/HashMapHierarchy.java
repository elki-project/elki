package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Centralized hierarchy implementation, using a HashMap of Lists.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type (arbitrary!)
 */
public class HashMapHierarchy<O> implements ModifiableHierarchy<O> {
  /**
   * Reference storage.
   */
  final private HashMap<O, Rec<O>> graph;

  /**
   * Constructor.
   */
  public HashMapHierarchy() {
    super();
    this.graph = new HashMap<>();
  }

  @Override
  public int size() {
    return graph.size();
  }

  @Override
  public void add(O parent, O child) {
    // Add child to parent.
    {
      Rec<O> rec = graph.get(parent);
      if (rec == null) {
        rec = new Rec<>();
        graph.put(parent, rec);
      }
      rec.addChild(child);
    }
    // Add child to parent
    {
      Rec<O> rec = graph.get(child);
      if (rec == null) {
        rec = new Rec<>();
        graph.put(child, rec);
      }
      rec.addParent(parent);
    }
  }

  @Override
  public void add(O entry) {
    Rec<O> rec = graph.get(entry);
    if (rec == null) {
      rec = new Rec<>();
      graph.put(entry, rec);
    }
  }

  @Override
  public void remove(O parent, O child) {
    // Remove child from parent.
    {
      Rec<O> rec = graph.get(parent);
      if (rec != null) {
        rec.removeChild(child);
      }
    }
    // Remove parent from child
    {
      Rec<O> rec = graph.get(child);
      if (rec != null) {
        rec.removeParent(parent);
      }
    }
  }

  @Override
  public void remove(O entry) {
    Rec<O> rec = graph.get(entry);
    if (rec == null) {
      return;
    }
    for (int i = 0; i < rec.nump; i++) {
      graph.get(rec.parents[i]).removeChild(entry);
      rec.parents[i] = null;
    }
    for (int i = 0; i < rec.numc; i++) {
      graph.get(rec.children[i]).removeParent(entry);
      rec.children[i] = null;
    }
    graph.remove(entry);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void removeSubtree(O entry) {
    Rec<O> rec = graph.get(entry);
    if (rec == null) {
      return;
    }
    for (int i = 0; i < rec.nump; i++) {
      graph.get(rec.parents[i]).removeChild(entry);
      rec.parents[i] = null;
    }
    for (int i = 0; i < rec.numc; i++) {
      final Rec<O> crec = graph.get(rec.children[i]);
      crec.removeParent(entry);
      if (crec.nump == 0) {
        removeSubtree((O) rec.children[i]);
      }
      rec.children[i] = null;
    }
  }

  @Override
  public int numChildren(O obj) {
    Rec<O> rec = graph.get(obj);
    if (rec == null) {
      return 0;
    }
    return rec.numc;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iter<O> iterChildren(O obj) {
    Rec<O> rec = graph.get(obj);
    if (rec == null) {
      return (Iter<O>) EMPTY_ITERATOR;
    }
    return rec.iterChildren();
  }

  @Override
  public Iter<O> iterDescendants(O obj) {
    return new ItrDesc(obj);
  }

  @Override
  public int numParents(O obj) {
    Rec<O> rec = graph.get(obj);
    if (rec == null) {
      return 0;
    }
    return rec.nump;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iter<O> iterParents(O obj) {
    Rec<O> rec = graph.get(obj);
    if (rec == null) {
      return (Iter<O>) EMPTY_ITERATOR;
    }
    return rec.iterParents();
  }

  @Override
  public Iter<O> iterAncestors(O obj) {
    return new ItrAnc(obj);
  }

  @Override
  public Iter<O> iterAll() {
    return new ItrAll();
  }

  /**
   * Hierarchy pointers for an object.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> object type
   */
  private static class Rec<O> {
    /**
     * Number of parents, number of children.
     */
    int nump = 0, numc = 0;

    /**
     * Parents.
     */
    Object[] parents = null;

    /**
     * Children.
     */
    Object[] children = null;

    /**
     * Add a parent.
     * 
     * @param parent Parent to add.
     */
    void addParent(O parent) {
      if (parents == null) {
        parents = new Object[1];
        parents[0] = parent;
        nump = 1;
      } else {
        for (int i = 0; i < nump; i++) {
          if (parent.equals(parents[i])) {
            return;
          }
        }
        if (parents.length == nump) {
          final int newsize = Math.min(5, (parents.length << 1) + 1);
          parents = Arrays.copyOf(parents, newsize);
        }
        parents[nump] = parent;
        nump++;
      }
    }

    /**
     * Add a child.
     * 
     * @param child Child to add
     */
    void addChild(O child) {
      if (children == null) {
        children = new Object[5];
        children[0] = child;
        numc = 1;
      } else {
        for (int i = 0; i < numc; i++) {
          if (child.equals(children[i])) {
            return;
          }
        }
        if (children.length == numc) {
          children = Arrays.copyOf(children, (children.length << 1) + 1);
        }
        children[numc] = child;
        numc++;
      }
    }

    /**
     * Remove a parent.
     * 
     * @param parent Parent to remove.
     */
    void removeParent(O parent) {
      if (parents == null) {
        return;
      }
      for (int i = 0; i < nump; i++) {
        if (parent.equals(parents[i])) {
          System.arraycopy(parents, i + 1, parents, i, nump - 1 - i);
          parents[nump] = null;
          nump--;
          break;
        }
      }
      if (nump == 0) {
        parents = null;
      }
    }

    /**
     * Remove a child.
     * 
     * @param child Child to remove.
     */
    void removeChild(O child) {
      if (children == null) {
        return;
      }
      for (int i = 0; i < numc; i++) {
        if (child.equals(children[i])) {
          System.arraycopy(children, i + 1, children, i, numc - 1 - i);
          children[numc] = null;
          numc--;
          break;
        }
      }
      if (numc == 0) {
        children = null;
      }
    }

    /**
     * Iterate over parents.
     * 
     * @return Iterator for parents.
     */
    @SuppressWarnings("unchecked")
    public Iter<O> iterParents() {
      if (nump == 0) {
        return (Iter<O>) EMPTY_ITERATOR;
      }
      return new ItrParents();
    }

    /**
     * Iterate over parents.
     * 
     * @return Iterator for parents.
     */
    @SuppressWarnings("unchecked")
    public Iter<O> iterChildren() {
      if (numc == 0) {
        return (Iter<O>) EMPTY_ITERATOR;
      }
      return new ItrChildren();
    }

    /**
     * Parent iterator.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    class ItrParents implements Iter<O> {
      int pos = 0;

      @Override
      public boolean valid() {
        return pos < nump;
      }

      @Override
      public void advance() {
        pos++;
      }

      @SuppressWarnings("unchecked")
      @Override
      public O get() {
        return (O) parents[pos];
      }
    }

    /**
     * Child iterator.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    class ItrChildren implements Iter<O> {
      int pos = 0;

      @Override
      public boolean valid() {
        return pos < numc;
      }

      @Override
      public void advance() {
        pos++;
      }

      @SuppressWarnings("unchecked")
      @Override
      public O get() {
        return (O) children[pos];
      }
    }
  }

  /**
   * Iterator to collect into the descendants.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrDesc implements Iter<O> {
    /**
     * Iterator over children
     */
    final Iter<O> childiter;

    /**
     * Iterator of current child
     */
    Iter<O> subiter = null;

    /**
     * Starting element.
     * 
     * @param start
     */
    ItrDesc(O start) {
      childiter = iterChildren(start);
    }

    @Override
    public boolean valid() {
      return childiter.valid() || (subiter != null && subiter.valid());
    }

    @Override
    public void advance() {
      if (subiter == null) { // Not yet descended
        assert (childiter.valid());
        subiter = iterDescendants(childiter.get());
      } else { // Continue with subtree
        subiter.advance();
      }
      if (subiter.valid()) {
        return;
      }
      // Proceed to next child.
      childiter.advance();
      subiter = null;
    }

    @Override
    public O get() {
      if (subiter != null) {
        assert (subiter.valid());
        return subiter.get();
      } else {
        assert (childiter.valid());
        return childiter.get();
      }
    }
  }

  /**
   * Iterator over all Ancestors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrAnc implements Iter<O> {
    /**
     * Iterator over children
     */
    final Iter<O> parentiter;

    /**
     * Iterator of current child
     */
    Iter<O> subiter = null;

    /**
     * Starting element.
     * 
     * @param start
     */
    ItrAnc(O start) {
      parentiter = iterParents(start);
    }

    @Override
    public boolean valid() {
      return parentiter.valid() || (subiter != null && subiter.valid());
    }

    @Override
    public void advance() {
      if (subiter == null) { // Not yet descended
        assert (parentiter.valid());
        subiter = iterAncestors(parentiter.get());
      } else { // Continue with subtree
        subiter.advance();
      }
      if (subiter.valid()) {
        return;
      }
      // Proceed to next child.
      parentiter.advance();
      subiter = null;
    }

    @Override
    public O get() {
      if (subiter != null) {
        assert (subiter.valid());
        return subiter.get();
      } else {
        assert (parentiter.valid());
        return parentiter.get();
      }
    }
  }

  /**
   * Iterator over all members of the hierarchy.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrAll implements Iter<O> {
    /**
     * The true iterator.
     */
    final Iterator<O> iter;

    /**
     * Current object.
     */
    O cur = null;

    /**
     * Constructor.
     */
    ItrAll() {
      iter = graph.keySet().iterator();
      advance();
    }

    @Override
    public boolean valid() {
      return cur != null;
    }

    @Override
    public void advance() {
      if (iter.hasNext()) {
        cur = iter.next();
      } else {
        cur = null;
      }
    }

    @Override
    public O get() {
      return cur;
    }
  }

  /**
   * Empty iterator.
   */
  private static final Iter<?> EMPTY_ITERATOR = new Iter<Object>() {
    @Override
    public boolean valid() {
      return false;
    }

    @Override
    public void advance() {
      throw new UnsupportedOperationException("Empty iterators must not be advanced.");
    }

    @Override
    public Object get() {
      throw new UnsupportedOperationException("Iterator is empty.");
    }
  };
}
