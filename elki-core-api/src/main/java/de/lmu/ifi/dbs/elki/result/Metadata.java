/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.result;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.EmptyIterator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Metadata management class.
 * <p>
 * This manages metadata in a global WeakHashMap. Prior to this, we had
 * dedicated Result types, which would store result hierarchy, names, etc. but
 * this caused quite an inheritance and dependency mess. But in particular, it
 * meant all objects returned needed to implement these interfaces.
 * <p>
 * Therefore, we are now experimenting storing metadata using a weak hash map,
 * so that it can be garbage collected, yet metadata can be attached to
 * arbitrary objects.
 * <p>
 * Be careful to not overuse this: as this class needs synchronization, there is
 * cost associated with retrieving the metadata of an object.
 * <p>
 * We also chose to not synchronize all operations; so concurrent changes to the
 * result hierarchy while traversing it may cause confusion.
 * <p>
 * If you store a WeakReference in the hierarchy, the iterators will
 * automatically call <code>get()</code>.
 * <p>
 * TODO: add a modCounter, and fail if concurrent modifications happen?
 *
 * @author Erich Schubert
 */
public class Metadata {
  /**
   * Get <b>or create</b> the Metadata of an object.
   *
   * @param o Object
   * @return Metadata
   */
  public static Metadata of(Object o) {
    synchronized(global) {
      Metadata ret = global.get(o);
      if(ret == null) {
        global.put(o, ret = new Metadata(o));
      }
      return ret;
    }
  }

  /**
   * Get <b>but do not create</b> the Metadata of an object.
   *
   * @param o Object
   * @return Metadata
   */
  public static Metadata get(Object o) {
    synchronized(global) {
      return global.get(o);
    }
  }

  /**
   * Get <b>or create</b> the Hierarchy of an object.
   *
   * @param o Object
   * @return Hierarchy
   */
  public static Hierarchy hierarchyOf(Object o) {
    return Metadata.of(o).hierarchy();
  }

  /**
   * The global metadata map.
   */
  private static final Map<Object, Metadata> global = new WeakHashMap<>();

  /**
   * Object owning the metadata.
   */
  private WeakReference<Object> owner;

  /**
   * Hierarchy information.
   */
  private Hierarchy hierarchy = new Hierarchy();

  /**
   * Human-readable name of the entry.
   */
  private String name;

  /**
   * Constructor, use via static methods only!
   *
   * @param o Object
   */
  private Metadata(Object o) {
    this.owner = new WeakReference<>(o);
  }

  /**
   * Access the objects hierarchy information.
   *
   * @return Hierarchy information
   */
  public Hierarchy hierarchy() {
    return hierarchy;
  }

  public void setLongName(String name) {
    this.name = name;
  }

  public String getLongName() {
    if(name != null) {
      return name;
    }
    Object owner = this.owner.get();
    if(owner == null) {
      return "<garbage collected>";
    }
    try {
      Method m = owner.getClass().getMethod("getLongName");
      if(m.getReturnType() == String.class) {
        return (String) m.invoke(owner);
      }
    }
    catch(SecurityException | IllegalArgumentException
        | ReflectiveOperationException e) {
      // pass.
    }
    return owner.getClass().getSimpleName();
  }

  /**
   * Empty list.
   */
  private static final Object[] EMPTY = new Object[0];

  /**
   * Automatically expand a reference.
   * 
   * @param ret Object
   * @return Referenced object (may be null!) or ret.
   */
  private static Object deref(Object ret) {
    return (ret instanceof Reference) ? ((Reference<?>) ret).get() : ret;
  }

  /**
   * Class to represent hierarchy information.
   *
   * @author Erich Schubert
   */
  public final class Hierarchy {
    /**
     * Number of parents.
     */
    int nump = 0;

    /**
     * Number of children.
     */
    int numc = 0;

    /**
     * Parents.
     */
    Object[] parents = EMPTY;

    /**
     * Children.
     */
    Object[] children = EMPTY;

    /**
     * Add a child to this node.
     *
     * @param c Child to add
     * @return {@code true} on success
     */
    public boolean addChild(Object c) {
      if(addChildInt(c)) {
        Metadata.of(c).hierarchy().addParentInt(Metadata.this.owner.get());
        ResultListenerList.resultAdded(c, Metadata.this.owner.get());
        return true;
      }
      return false;
    }

    /**
     * Add a weak child to this node, which may be automatically garbage
     * collected.
     *
     * @param c Child to add
     * @return {@code true} on success
     */
    public boolean addWeakChild(Object c) {
      if(addChildInt(new WeakReference<>(c))) {
        Metadata.of(c).hierarchy().addParentInt(Metadata.this.owner.get());
        ResultListenerList.resultAdded(c, Metadata.this.owner.get());
        return true;
      }
      return false;
    }

    /**
     * Remove a child from this node.
     *
     * @param c Child to remove
     * @return {@code true} on success
     */
    public boolean removeChild(Object c) {
      if(removeChildInt(c)) {
        Metadata.of(c).hierarchy().removeParentInt(Metadata.this.owner.get());
        ResultListenerList.resultRemoved(c, Metadata.this.owner.get());
        return true;
      }
      return false;
    }

    /**
     * Add a parent.
     *
     * @param parent Parent to add.
     * @return {@code true} when changed
     */
    synchronized private boolean addParentInt(Object parent) {
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
    synchronized private boolean addChildInt(Object child) {
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
    synchronized private boolean removeParentInt(Object parent) {
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
    synchronized private boolean removeChildInt(Object child) {
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
     * Get the number of parents.
     *
     * @return Number of parents
     */
    public int numParents() {
      return nump;
    }

    /**
     * Get the number of children.
     *
     * @return Number of children
     */
    public int numChildren() {
      return numc;
    }

    /**
     * Iterate over parents.
     *
     * @return Iterator for parents
     */
    public It<Object> iterParents() {
      return (nump == 0) ? EmptyIterator.empty() : new ItrParents();
    }

    /**
     * Iterate over parents, backwards.
     *
     * @return Iterator for parents
     */
    public It<Object> iterParentsReverse() {
      return (nump == 0) ? EmptyIterator.empty() : new ItrParentsReverse();
    }

    /**
     * Iterate over all ancestors.
     *
     * @return Iterator for descendants
     */
    public It<Object> iterAncestors() {
      return (nump == 0) ? EmptyIterator.empty() : new ItrAnc();
    }

    /**
     * Iterate over all ancestors and the object itself.
     *
     * @return Iterator for descendants
     */
    public It<Object> iterAncestorsSelf() {
      return new ItrAnc(owner.get());
    }

    /**
     * Iterate over children.
     *
     * @return Iterator for children
     */
    public It<Object> iterChildren() {
      return (numc == 0) ? EmptyIterator.empty() : new ItrChildren();
    }

    /**
     * Iterate over children, backwards.
     *
     * @return Iterator for children
     */
    public It<Object> iterChildrenReverse() {
      return (numc == 0) ? EmptyIterator.empty() : new ItrChildrenReverse();
    }

    /**
     * Iterate over all descendants.
     *
     * @return Iterator for descendants
     */
    public It<Object> iterDescendants() {
      return (numc == 0) ? EmptyIterator.empty() : new ItrDesc();
    }

    /**
     * Iterate over all descendants and the object itself.
     *
     * @return Iterator for descendants
     */
    public It<Object> iterDescendantsSelf() {
      return new ItrDesc(owner.get());
    }

    /**
     * Parent iterator.
     *
     * @author Erich Schubert
     */
    private class ItrParents extends EagerIt<Object> {
      /**
       * Next position.
       */
      private int pos = 0;

      /**
       * Constructor.
       */
      public ItrParents() {
        advance();
      }

      @Override
      public It<Object> advance() {
        current = null;
        if(pos < nump) {
          current = deref(parents[pos++]);
          assert (current != null); // Only children may be weak
        }
        return this;
      }

    }

    /**
     * Parent iterator.
     *
     * @author Erich Schubert
     */
    private class ItrParentsReverse extends EagerIt<Object> {
      /**
       * Current position.
       */
      private int pos = nump;

      /**
       * Constructor.
       */
      public ItrParentsReverse() {
        advance();
      }

      @Override
      public It<Object> advance() {
        current = null;
        if(pos > 0) {
          current = deref(parents[--pos]);
          assert (current != null); // Only children may be weak
        }
        return this;
      }
    }

    /**
     * Child iterator.
     *
     * @author Erich Schubert
     */
    private class ItrChildren extends EagerIt<Object> {
      /**
       * Next position.
       */
      private int pos = 0;

      /**
       * Constructor.
       */
      public ItrChildren() {
        advance();
      }

      @Override
      public It<Object> advance() {
        current = null;
        while(pos < numc) {
          current = deref(children[pos++]);
          if(current != null) {
            return this;
          }
          // Expired weak reference detected.
          System.arraycopy(children, pos, children, pos - 1, numc - pos);
          children[--numc] = null;
          --pos;
        }
        return this;
      }
    }

    /**
     * Child iterator.
     *
     * @author Erich Schubert
     */
    private class ItrChildrenReverse extends EagerIt<Object> {
      /**
       * Current position.
       */
      private int pos = numc;

      /**
       * Constructor.
       */
      public ItrChildrenReverse() {
        advance();
      }

      @Override
      public It<Object> advance() {
        current = null;
        while(pos > 0) {
          current = deref(children[--pos]);
          if(current != null) {
            return this;
          }
          // Expired weak reference detected.
          System.arraycopy(children, pos + 1, children, pos, numc - pos);
          children[--numc] = null;
          pos++;
        }
        return this;
      }
    }

    /**
     * Iterator over all descendants.
     *
     * @author Erich Schubert
     */
    private class ItrDesc implements It<Object> {
      /**
       * Iterator over children
       */
      final It<Object> childiter;

      /**
       * Iterator of current child
       */
      It<Object> subiter = null;

      /**
       * Additional object to return as first result.
       */
      Object extra = null;

      /**
       * Constructor for descendants-only.
       */
      ItrDesc() {
        this(null);
      }

      /**
       * Constructor with additional element.
       *
       * @param extra Additional element (cannot be {@code null}).
       */
      ItrDesc(Object extra) {
        this.childiter = new ItrChildren();
        this.extra = extra;
      }

      @Override
      public boolean valid() {
        return extra != null || childiter.valid() || (subiter != null && subiter.valid());
      }

      @Override
      public It<Object> advance() {
        if(extra != null) {
          extra = null;
          return this;
        }
        if(subiter == null) { // Not yet descended
          assert (childiter.valid());
          subiter = Metadata.of(childiter.get()).hierarchy.new ItrDesc();
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
      public Object get() {
        if(extra != null) {
          return extra;
        }
        if(subiter != null) {
          assert (subiter.valid());
          return subiter.get();
        }
        assert (childiter.valid());
        return childiter.get();
      }
    }

    /**
     * Iterator over all Ancestors.
     *
     * @author Erich Schubert
     */
    private class ItrAnc implements It<Object> {
      /**
       * Iterator over children
       */
      final It<Object> parentiter;

      /**
       * Iterator of current child
       */
      It<Object> subiter = null;

      /**
       * Additional object to return as first result.
       */
      Object extra = null;

      /**
       * Constructor for descendants-only.
       */
      ItrAnc() {
        this(null);
      }

      /**
       * Constructor with additional element.
       *
       * @param extra Additional element (cannot be {@code null}).
       */
      ItrAnc(Object extra) {
        parentiter = new ItrParents();
        this.extra = extra;
      }

      @Override
      public boolean valid() {
        return extra != null || parentiter.valid() || (subiter != null && subiter.valid());
      }

      @Override
      public It<Object> advance() {
        if(extra != null) {
          extra = null;
          return this;
        }
        if(subiter == null) { // Not yet descended
          assert (parentiter.valid());
          subiter = Metadata.of(parentiter.get()).hierarchy().new ItrAnc();
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
      public Object get() {
        if(extra != null) {
          return extra;
        }
        if(subiter != null) {
          assert (subiter.valid());
          return subiter.get();
        }
        assert (parentiter.valid());
        return parentiter.get();
      }
    }
  }

  /**
   * Base class for iterators that need to look ahead, e.g. to check
   * conditions on the next element.
   *
   * @author Erich Schubert
   *
   * @param <O>
   */
  private static abstract class EagerIt<O> implements It<O> {
    /**
     * Next object to return.
     */
    protected Object current = null;

    @Override
    public boolean valid() {
      return current != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public O get() {
      return (O) current;
    }
  }
}
