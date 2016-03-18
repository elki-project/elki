package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

/**
 * Centralized hierarchy implementation, using a HashMap of Lists.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type (arbitrary!)
 */
public class HashMapHierarchy<O> implements ModifiableHierarchy<O> {
  /**
   * Reference storage.
   */
  final private HashMap<O, Rec<O>> graph;

  /**
   * All elements, in insertion order (and will not fail badly if concurrent
   * insertions happen).
   */
  Object[] elems = new Object[11];

  /**
   * Number of all elements.
   */
  int numelems = 0;

  /**
   * Constructor.
   */
  public HashMapHierarchy() {
    super();
    this.graph = new HashMap<>();
  }

  @Override
  public boolean contains(O object) {
    return graph.containsKey(object);
  }

  @Override
  public int size() {
    return graph.size();
  }

  @Override
  public boolean add(O parent, O child) {
    boolean changed = false;
    // Add child to parent.
    {
      Rec<O> rec = getRec(parent);
      if(rec == null) {
        rec = new Rec<>();
        putRec(parent, rec);
      }
      changed |= rec.addChild(child);
    }
    // Add child to parent
    {
      Rec<O> rec = getRec(child);
      if(rec == null) {
        rec = new Rec<>();
        putRec(child, rec);
      }
      changed |= rec.addParent(parent);
    }
    return changed;
  }

  @Override
  public boolean add(O entry) {
    Rec<O> rec = getRec(entry);
    if(rec == null) {
      rec = new Rec<>();
      putRec(entry, rec);
      return true;
    }
    return false;
  }

  @Override
  public boolean remove(O parent, O child) {
    boolean changed = false;
    // Remove child from parent.
    {
      Rec<O> rec = getRec(parent);
      if(rec != null) {
        changed |= rec.removeChild(child);
      }
    }
    // Remove parent from child
    {
      Rec<O> rec = getRec(child);
      if(rec != null) {
        changed |= rec.removeParent(parent);
      }
    }
    return changed;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean remove(O entry) {
    Rec<O> rec = getRec(entry);
    if(rec == null) {
      return false;
    }
    for(int i = 0; i < rec.nump; i++) {
      getRec((O) rec.parents[i]).removeChild(entry);
      rec.parents[i] = null;
    }
    for(int i = 0; i < rec.numc; i++) {
      getRec((O) rec.children[i]).removeParent(entry);
      rec.children[i] = null;
    }
    removeRec(entry);
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean removeSubtree(O entry) {
    Rec<O> rec = getRec(entry);
    if(rec == null) {
      return false;
    }
    for(int i = 0; i < rec.nump; i++) {
      getRec((O) rec.parents[i]).removeChild(entry);
      rec.parents[i] = null;
    }
    for(int i = 0; i < rec.numc; i++) {
      final Rec<O> crec = getRec((O) rec.children[i]);
      crec.removeParent(entry);
      if(crec.nump == 0) {
        removeSubtree((O) rec.children[i]);
      }
      rec.children[i] = null;
    }
    removeRec(entry);
    return true;
  }

  @Override
  public int numChildren(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return 0;
    }
    return rec.numc;
  }

  @Override
  public Iter<O> iterChildren(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return emptyIterator();
    }
    return rec.iterChildren();
  }

  @Override
  public Iter<O> iterChildrenReverse(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return emptyIterator();
    }
    return rec.iterChildrenReverse();
  }

  @Override
  public Iter<O> iterDescendants(O obj) {
    return new ItrDesc(obj);
  }

  @Override
  public Iter<O> iterDescendantsSelf(O obj) {
    return new ItrDesc(obj, obj);
  }

  @Override
  public int numParents(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return 0;
    }
    return rec.nump;
  }

  @Override
  public Iter<O> iterParents(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return emptyIterator();
    }
    return rec.iterParents();
  }

  @Override
  public Iter<O> iterParentsReverse(O obj) {
    Rec<O> rec = getRec(obj);
    if(rec == null) {
      return emptyIterator();
    }
    return rec.iterParentsReverse();
  }

  @Override
  public Iter<O> iterAncestors(O obj) {
    return new ItrAnc(obj);
  }

  @Override
  public Iter<O> iterAncestorsSelf(O obj) {
    return new ItrAnc(obj, obj);
  }

  @Override
  public Iter<O> iterAll() {
    return new ItrAll();
  }

  /**
   * Get a record.
   *
   * @param obj Key
   * @return Record
   */
  private Rec<O> getRec(O obj) {
    return graph.get(obj);
  }

  /**
   * Put a record.
   *
   * @param obj Key
   * @param rec Record
   */
  private void putRec(O obj, Rec<O> rec) {
    graph.put(obj, rec);
    for(int i = 0; i < numelems; ++i) {
      if(obj == elems[i]) {
        return;
      }
    }
    if(elems.length == numelems) {
      elems = Arrays.copyOf(elems, (elems.length << 1) + 1);
    }
    elems[numelems++] = obj;
  }

  /**
   * Remove a record.
   *
   * @param obj Key
   */
  private void removeRec(O obj) {
    graph.remove(obj);
    for(int i = 0; i < numelems; ++i) {
      if(obj == elems[i]) {
        System.arraycopy(elems, i + 1, elems, i, --numelems - i);
        elems[numelems] = null;
        return;
      }
    }
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
  protected static class Rec<O> {
    /**
     * Number of parents, number of children.
     */
    int nump = 0, numc = 0;

    /**
     * Parents.
     */
    Object[] parents = EMPTY;

    /**
     * Children.
     */
    Object[] children = EMPTY;

    /**
     * Empty list.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * Add a parent.
     *
     * @param parent Parent to add.
     * @return {@code true} when changed
     */
    boolean addParent(O parent) {
      if(parents == EMPTY) {
        parents = new Object[1];
        parents[0] = parent;
        nump = 1;
        return true;
      }
      for(int i = 0; i < nump; i++) {
        if(parent.equals(parents[i])) {
          return false; // Exists already.
        }
      }
      if(parents.length == nump) {
        final int newsize = Math.max(5, (parents.length << 1) + 1);
        parents = Arrays.copyOf(parents, newsize);
      }
      parents[nump++] = parent;
      return true;
    }

    /**
     * Add a child.
     *
     * @param child Child to add
     * @return {@code true} when changed
     */
    boolean addChild(O child) {
      if(children == EMPTY) {
        children = new Object[5];
        children[0] = child;
        numc = 1;
        return true;
      }
      for(int i = 0; i < numc; i++) {
        if(child.equals(children[i])) {
          return false; // Exists already
        }
      }
      if(children.length == numc) {
        children = Arrays.copyOf(children, (children.length << 1) + 1);
      }
      children[numc++] = child;
      return true;
    }

    /**
     * Remove a parent.
     *
     * @param parent Parent to remove.
     * @return {@code true} when changed
     */
    boolean removeParent(O parent) {
      if(parents == EMPTY) {
        return false;
      }
      for(int i = 0; i < nump; i++) {
        if(parent.equals(parents[i])) {
          --nump;
          System.arraycopy(parents, i + 1, parents, i, nump - i);
          parents[nump] = null;
          if(nump == 0) {
            parents = EMPTY;
          }
          return true;
        }
      }
      return false;
    }

    /**
     * Remove a child.
     *
     * @param child Child to remove.
     * @return {@code true} when changed
     */
    boolean removeChild(O child) {
      if(children == EMPTY) {
        return false;
      }
      for(int i = 0; i < numc; i++) {
        if(child.equals(children[i])) {
          --numc;
          System.arraycopy(children, i + 1, children, i, numc - i);
          children[numc] = null;
          if(numc == 0) {
            children = EMPTY;
          }
          return true;
        }
      }
      return false;
    }

    /**
     * Iterate over parents.
     *
     * @return Iterator for parents.
     */
    @SuppressWarnings("unchecked")
    public Iter<O> iterParents() {
      if(nump == 0) {
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
    public Iter<O> iterParentsReverse() {
      if(nump == 0) {
        return (Iter<O>) EMPTY_ITERATOR;
      }
      return new ItrParentsReverse();
    }

    /**
     * Iterate over parents.
     *
     * @return Iterator for parents.
     */
    @SuppressWarnings("unchecked")
    public Iter<O> iterChildren() {
      if(numc == 0) {
        return (Iter<O>) EMPTY_ITERATOR;
      }
      return new ItrChildren();
    }

    /**
     * Iterate over parents.
     *
     * @return Iterator for parents.
     */
    @SuppressWarnings("unchecked")
    public Iter<O> iterChildrenReverse() {
      if(numc == 0) {
        return (Iter<O>) EMPTY_ITERATOR;
      }
      return new ItrChildrenReverse();
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
      public Iter<O> advance() {
        pos++;
        return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public O get() {
        return (O) parents[pos];
      }
    }

    /**
     * Parent iterator.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    class ItrParentsReverse implements Iter<O> {
      int pos = nump - 1;

      @Override
      public boolean valid() {
        return pos >= 0;
      }

      @Override
      public Iter<O> advance() {
        pos--;
        return this;
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
      public Iter<O> advance() {
        pos++;
        return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public O get() {
        return (O) children[pos];
      }
    }

    /**
     * Child iterator.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    class ItrChildrenReverse implements Iter<O> {
      int pos = numc - 1;

      @Override
      public boolean valid() {
        return pos >= 0;
      }

      @Override
      public Iter<O> advance() {
        pos--;
        return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public O get() {
        return (O) children[pos];
      }
    }
  }

  /**
   * Iterator over all descendants.
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
     * Additional object to return as first result.
     */
    O extra = null;

    /**
     * Constructor for descendants-only.
     *
     * @param start Starting element.
     */
    ItrDesc(O start) {
      this(start, null);
    }

    /**
     * Constructor with additional element.
     *
     * @param start Starting element.
     * @param extra Additional element (cannot be {@code null}).
     */
    ItrDesc(O start, O extra) {
      childiter = iterChildren(start);
      this.extra = extra;
    }

    @Override
    public boolean valid() {
      return extra != null || childiter.valid() || (subiter != null && subiter.valid());
    }

    @Override
    public Iter<O> advance() {
      if(extra != null) {
        extra = null;
        return this;
      }
      if(subiter == null) { // Not yet descended
        assert(childiter.valid());
        subiter = iterDescendants(childiter.get());
      }
      else { // Continue with subtree
        subiter.advance();
      }
      if(subiter.valid()) {
        return this;
      }
      // Proceed to next child.
      childiter.advance();
      subiter = null;
      return this;
    }

    @Override
    public O get() {
      if(extra != null) {
        return extra;
      }
      if(subiter != null) {
        assert(subiter.valid());
        return subiter.get();
      }
      assert(childiter.valid());
      return childiter.get();
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
     * Additional object to return as first result.
     */
    O extra = null;

    /**
     * Constructor for descendants-only.
     *
     * @param start Starting element.
     */
    ItrAnc(O start) {
      this(start, null);
    }

    /**
     * Constructor with additional element.
     *
     * @param start Starting element.
     * @param extra Additional element (cannot be {@code null}).
     */
    ItrAnc(O start, O extra) {
      parentiter = iterParents(start);
      this.extra = extra;
    }

    @Override
    public boolean valid() {
      return extra != null || parentiter.valid() || (subiter != null && subiter.valid());
    }

    @Override
    public Iter<O> advance() {
      if(extra != null) {
        extra = null;
        return this;
      }
      if(subiter == null) { // Not yet descended
        assert(parentiter.valid());
        subiter = iterAncestors(parentiter.get());
      }
      else { // Continue with subtree
        subiter.advance();
      }
      if(subiter.valid()) {
        return this;
      }
      // Proceed to next child.
      parentiter.advance();
      subiter = null;
      return this;
    }

    @Override
    public O get() {
      if(extra != null) {
        return extra;
      }
      if(subiter != null) {
        assert(subiter.valid());
        return subiter.get();
      }
      assert(parentiter.valid());
      return parentiter.get();
    }
  }

  /**
   * Iterator over all known elements, by insertion order.
   *
   * @author Erich Schubert
   */
  private class ItrAll implements Hierarchy.Iter<O> {
    int pos;

    @Override
    public boolean valid() {
      return pos < numelems;
    }

    @SuppressWarnings("unchecked")
    @Override
    public O get() {
      return (O) elems[pos];
    }

    @Override
    public ItrAll advance() {
      ++pos;
      return this;
    }
  }

  /**
   * Get an empty hierarchy iterator.
   *
   * @return Empty iterator
   */
  @SuppressWarnings("unchecked")
  public static <O> Hierarchy.Iter<O> emptyIterator() {
    return (Iter<O>) EMPTY_ITERATOR;
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
    public Iter<Object> advance() {
      throw new UnsupportedOperationException("Empty iterators must not be advanced.");
    }

    @Override
    public Object get() {
      throw new UnsupportedOperationException("Iterator is empty.");
    }
  };
}
