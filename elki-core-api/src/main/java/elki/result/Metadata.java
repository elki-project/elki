/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.result;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import elki.logging.Logging;
import elki.utilities.datastructures.iterator.EmptyIterator;
import elki.utilities.datastructures.iterator.It;

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
 * @since 0.8.0
 */
public class Metadata extends WeakReference<Object> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(Metadata.class);

  /**
   * Reference queue for CLEANER.
   */
  private static ReferenceQueue<? super Object> queue = new ReferenceQueue<>();

  /**
   * Cleanup thread.
   */
  private static CleanerThread CLEANER;

  /**
   * Cleanup thread.
   *
   * @author Erich Schubert
   */
  private static class CleanerThread extends Thread {
    public CleanerThread() {
      super("ELKI Garbage Collection");
      setDaemon(true); // Don't prevent program termination
    }

    @Override
    public void run() {
      LOG.debugFinest("Garbage collection thread started.");
      while(true) {
        try {
          ((Metadata) queue.remove()).cleanup();
        }
        catch(InterruptedException e) {
          // Ok to stop.
        }
        synchronized(global) {
          if(global.isEmpty()) {
            LOG.debugFinest("Garbage collection thread has quit.");
            return;
          }
        }
      }
    };
  };

  /**
   * Get <b>or create</b> the Metadata of an object.
   *
   * @param o Object
   * @return Metadata
   */
  public static Metadata of(Object o) {
    assert !(o instanceof Reference);
    expungeStaleEntries();
    synchronized(global) {
      return global.computeIfAbsent(o, x -> {
        if(CLEANER == null || !CLEANER.isAlive()) {
          CLEANER = new CleanerThread();
          CLEANER.start();
        }
        return new Metadata(o);
      });
    }
  }

  /**
   * Get <b>but do not create</b> the Metadata of an object.
   *
   * @param o Object
   * @return Metadata
   */
  public static Metadata get(Object o) {
    assert !(o instanceof Reference);
    expungeStaleEntries();
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
    return of(o).hierarchy();
  }

  /**
   * The global metadata map.
   */
  private static final Map<Object, Metadata> global = new WeakHashMap<>();

  /**
   * Hierarchy information.
   */
  private Hierarchy hierarchy = new Hierarchy();

  /**
   * Attached result listeners
   */
  private ArrayList<ResultListener> listeners = null;

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
    super(o, queue);
    // For debugging garbage collection below...
    if(LOG.isDebuggingFine()) {
      name = getLongName();
      name = name != null ? name : o.toString();
    }
    expungeStaleEntries();
  }

  /**
   * Expunges stale entries from the table.
   */
  public static void expungeStaleEntries() {
    for(Metadata x; (x = (Metadata) queue.poll()) != null;) {
      x.cleanup();
    }
    global.size(); // Trigger CLEANER in weak hash map, too.
  }

  /**
   * Cleanup function to help garbage collection.
   */
  private void cleanup() {
    listeners = null;
    if(hierarchy.numc > 0) {
      if(LOG.isDebuggingFine()) {
        String nam = getLongName();
        nam = nam != null ? nam : get() != null ? get().toString() : "<garbage collected>";
        LOG.debugFinest("Garbage collecting: " + nam);
      }
      synchronized(global) {
        for(int i = hierarchy.numc - 1; i >= 0; i--) {
          Metadata ret = global.get(hierarchy.children[i]);
          if(ret != null) {
            ret.hierarchy().removeParentInt(this);
          }
        }
      }
      hierarchy.numc = 0;
      Arrays.fill(hierarchy.children, null);
    }
    hierarchy.nump = 0;
    Arrays.fill(hierarchy.parents, null);
  }

  /**
   * Access the objects hierarchy information.
   *
   * @return Hierarchy information
   */
  public Hierarchy hierarchy() {
    return hierarchy;
  }

  /**
   * Set the long name of a result.
   *
   * @param name Result name
   */
  public void setLongName(String name) {
    this.name = name;
  }

  /**
   * Get the long name of a result.
   *
   * @return name
   */
  public String getLongName() {
    if(name != null) {
      return name;
    }
    Object owner = this.get();
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
   * Automatically expand a reference.
   * 
   * @param ret Object
   * @return Referenced object (may be null!) or ret.
   */
  private static Object deref(Object ret) {
    return (ret instanceof Reference) ? ((Reference<?>) ret).get() : ret;
  }

  /**
   * Empty list.
   */
  private static final Object[] EMPTY_CHILDREN = new Object[0];

  /**
   * Empty list.
   */
  private static final Metadata[] EMPTY_PARENTS = new Metadata[0];

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
    private Metadata[] parents = EMPTY_PARENTS;

    /**
     * Children.
     */
    private Object[] children = EMPTY_CHILDREN;

    /**
     * Add a child to this node.
     *
     * @param c Child to add
     * @return {@code true} on success
     */
    public boolean addChild(Object c) {
      Object o = Metadata.this.get();
      assert o != c;
      if(o != null && addChildInt(c)) {
        Metadata.of(c).hierarchy().addParentInt(Metadata.this);
        Metadata.of(o).notifyChildAdded(c);
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
      Object o = Metadata.this.get();
      assert o != c;
      if(o != null && addChildInt(new WeakReference<>(c))) {
        Metadata.of(c).hierarchy().addParentInt(Metadata.this);
        Metadata.of(o).notifyChildAdded(c);
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
        Metadata.of(c).hierarchy().removeParentInt(Metadata.this);
        final Object o = Metadata.this.get();
        if(o != null) {
          Metadata.of(o).notifyChildRemoved(c);
        }
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
    private synchronized boolean addParentInt(Metadata parent) {
      final Object p = parent.get();
      if(p == null) {
        return false;
      }
      if(parents == EMPTY_CHILDREN) {
        parents = new Metadata[] { parent };
        nump = 1;
        return true;
      }
      for(int i = 0; i < nump; i++) {
        if(parent == parents[i] || p == parents[i].get()) {
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
    private synchronized boolean addChildInt(Object child) {
      if(children == EMPTY_CHILDREN) {
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
    private synchronized boolean removeParentInt(Metadata parent) {
      if(parents == EMPTY_PARENTS) {
        return false;
      }
      Object p = parent.get();
      for(int i = 0; i < nump; i++) {
        if(parent == parents[i] || (p != null && p == parents[i].get())) {
          if(--nump == 0) {
            parents = EMPTY_PARENTS;
          }
          else {
            System.arraycopy(parents, i + 1, parents, i, nump - i);
            parents[nump] = null;
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
    private synchronized boolean removeChildInt(Object child) {
      if(children == EMPTY_CHILDREN) {
        return false;
      }
      for(int i = 0; i < numc; i++) {
        Object ci = children[i];
        if(child.equals(ci) || ci instanceof Reference && child.equals(((Reference<?>) ci).get())) {
          if(--numc == 0) {
            children = EMPTY_CHILDREN;
          }
          else {
            System.arraycopy(children, i + 1, children, i, numc - i);
            children[numc] = null;
          }
          return true;
        }
      }
      return false;
    }

    /**
     * Get the number of children.
     *
     * @return {@code true} if the code has children
     */
    public boolean hasChildren() {
      return numc > 0;
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
      return new ItrAnc(Metadata.this.get());
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
      return new ItrDesc(Metadata.this.get());
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
          System.arraycopy(children, pos + 1, children, pos, numc - pos - 1);
          children[--numc] = null;
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
        this.childiter = iterChildren();
        this.extra = extra;
      }

      @Override
      public boolean valid() {
        return extra != null || (subiter != null && subiter.valid()) || lookahead();
      }

      @Override
      public It<Object> advance() {
        lookahead();
        if(extra != null) {
          extra = null;
          return this;
        }
        if(subiter != null && subiter.valid()) {
          subiter.advance();
        }
        return this;
      }

      private boolean lookahead() {
        while(true) {
          if(extra != null || (subiter != null && subiter.valid())) {
            return true; // Next is available.
          }
          if(!childiter.valid()) {
            return false;
          }
          extra = childiter.get();
          childiter.advance();
          subiter = extra == null ? null : Metadata.of(extra).hierarchy.iterDescendants();
        }
      }

      @Override
      public Object get() {
        lookahead(); // In case someone did not call valid()
        return extra != null ? extra : //
            subiter != null ? subiter.get() : null;
      }
    }

    /**
     * Iterator over all ancestors.
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
        this.parentiter = iterParents();
        this.extra = extra;
      }

      @Override
      public boolean valid() {
        return extra != null || (subiter != null && subiter.valid()) || lookahead();
      }

      @Override
      public It<Object> advance() {
        lookahead();
        if(extra != null) {
          extra = null;
          return this;
        }
        if(subiter != null && subiter.valid()) {
          subiter.advance();
        }
        return this;
      }

      private boolean lookahead() {
        while(true) {
          if(extra != null || (subiter != null && subiter.valid())) {
            return true; // Next is available.
          }
          if(!parentiter.valid()) {
            return false;
          }
          extra = parentiter.get();
          parentiter.advance();
          subiter = extra == null ? null : Metadata.of(extra).hierarchy.iterAncestors();
        }
      }

      @Override
      public Object get() {
        lookahead(); // In case someone did not call valid()
        return extra != null ? extra : //
            subiter != null ? subiter.get() : null;
      }
    }
  }

  /**
   * Register a result listener.
   *
   * @param listener Result listener.
   */
  public void addResultListener(ResultListener listener) {
    if(listeners == null) {
      listeners = new ArrayList<>();
    }
    for(int i = 0; i < listeners.size(); i++) {
      if(listeners.get(i) == listener) {
        return;
      }
    }
    listeners.add(listener);
  }

  /**
   * Remove a result listener.
   *
   * @param listener Result listener.
   */
  public void removeResultListener(ResultListener listener) {
    if(listeners != null) {
      listeners.remove(listener);
    }
  }

  /**
   * Informs all registered {@link ResultListener} that a new result was added.
   *
   * @param child New child result added
   */
  private void notifyChildAdded(Object child) {
    Object parent = this.get();
    if(parent == null || child == null) {
      return;
    }
    if(LOG.isDebugging()) {
      LOG.debug("Result added: " + child + " <- " + parent);
    }
    doNotify(x -> x.resultAdded(child, parent));
  }

  /**
   * Informs all registered {@link ResultListener} that a result has changed.
   */
  public void notifyChanged() {
    Object current = this.get();
    if(current == null) {
      return;
    }
    if(LOG.isDebugging()) {
      LOG.debug("Result changed: " + current);
    }
    doNotify(x -> x.resultChanged(current));
  }

  /**
   * Informs all registered {@link ResultListener} that a new result has been
   * removed.
   *
   * @param child result that has been removed
   */
  private void notifyChildRemoved(Object child) {
    Object parent = this.get();
    if(LOG.isDebugging()) {
      LOG.debug("Result removed: " + child + " <- " + parent);
    }
    doNotify(x -> x.resultRemoved(child, parent));
  }

  /**
   * Notify, also via all parent listeners.
   *
   * @param f Listener consumer
   */
  private void doNotify(Consumer<ResultListener> f) {
    if(listeners != null) {
      for(int i = listeners.size(); --i >= 0;) {
        f.accept(listeners.get(i));
      }
    }
    for(It<Object> it = hierarchy.iterAncestors(); it.valid(); it.advance()) {
      ArrayList<ResultListener> l = Metadata.of(it.get()).listeners;
      if(l != null) {
        for(int i = l.size(); --i >= 0;) {
          f.accept(l.get(i));
        }
      }
    }
  }

  /**
   * Base class for iterators that need to look ahead, e.g., to check
   * conditions on the next element.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  private abstract static class EagerIt<O> implements It<O> {
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
