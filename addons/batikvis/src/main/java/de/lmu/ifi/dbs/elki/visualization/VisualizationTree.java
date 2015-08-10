package de.lmu.ifi.dbs.elki.visualization;
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

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HashMapHierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;

/**
 * Tree - actually a forest - to manage visualizations.
 *
 * @author Erich Schubert
 *
 * @apiviz.uses Handler1
 * @apiviz.uses Handler2
 * @apiviz.uses Handler3
 */
public class VisualizationTree extends HashMapHierarchy<Object> {
  /**
   * Constructor.
   */
  public VisualizationTree() {
    super();
  }

  /**
   * Filtered iterator.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   *
   * @apiviz.exclude
   */
  public static class StackedIter<B, A extends B> implements Hierarchy.Iter<B> {
    /**
     * Iterator in primary hierarchy.
     */
    private Hierarchy.Iter<? extends A> it1;

    /**
     * Secondary hierarchy.
     */
    private Hierarchy<B> hier2;

    /**
     * Iterator in secondary hierarchy.
     */
    private Hierarchy.Iter<B> it2;

    /**
     * Constructor.
     *
     * @param it1 Iterator in primary hierarchy
     * @param hier2 Iterator in secondary hierarchy
     */
    public StackedIter(Hierarchy.Iter<? extends A> it1, Hierarchy<B> hier2) {
      this.it1 = it1;
      this.hier2 = hier2;
      if(it1.valid()) {
        this.it2 = hier2.iterDescendants(it1.get());
        it1.advance();
      }
      else {
        this.it2 = null;
      }
    }

    @Override
    public B get() {
      return it2.get();
    }

    @Override
    public boolean valid() {
      return it2.valid();
    }

    @Override
    public StackedIter<B, A> advance() {
      if(it2.valid()) {
        it2.advance();
      }
      while(!it2.valid() && it1.valid()) {
        it2 = hier2.iterDescendants(it1.get());
        it1.advance();
      }
      return this;
    }
  }

  /**
   * Filtered iterator.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   *
   * @param <O> Object type
   */
  public static class FilteredIter<O> implements Hierarchy.Iter<O> {
    /**
     * Class filter.
     */
    Class<O> filter;

    /**
     * Current object, if valid.
     */
    O current;

    /**
     * Iterator in primary hierarchy.
     */
    private Hierarchy.Iter<?> it;

    /**
     * Constructor.
     *
     * @param it Iterator in primary hierarchy
     * @param clazz Class filter
     */
    public FilteredIter(Hierarchy.Iter<?> it, Class<O> clazz) {
      this.it = it;
      this.filter = clazz;
      this.next();
    }

    @Override
    public O get() {
      return current;
    }

    @Override
    public boolean valid() {
      return current != null;
    }

    @Override
    public FilteredIter<O> advance() {
      it.advance();
      next();
      return this;
    }

    /**
     * Java iterator style, because we need to "peek" the next element.
     */
    private void next() {
      while(it.valid()) {
        Object o = it.get();
        it.advance();
        if(filter.isInstance(o)) {
          current = filter.cast(o);
          return;
        }
      }
      current = null;
    }
  }

  /**
   * Handler for a single result.
   *
   * @author Erich Schubert
   *
   * @param <A> Object type
   */
  public static interface Handler1<A> {
    /**
     * Process a new result.
     *
     * @param context Context
     * @param result First result
     */
    void process(VisualizerContext context, A result);
  }

  /**
   * Handler for two result.
   *
   * @author Erich Schubert
   *
   * @param <A> Object type
   * @param <B> Object type
   */
  public static interface Handler2<A, B> {
    /**
     * Process a new result.
     *
     * @param context Context
     * @param result First result
     * @param result2 Second result
     */
    void process(VisualizerContext context, A result, B result2);
  }

  /**
   * Handler for three result.
   *
   * @author Erich Schubert
   *
   * @param <A> Object type
   * @param <B> Object type
   * @param <C> Object type
   */
  public static interface Handler3<A, B, C> {
    /**
     * Process a new result.
     *
     * @param context Context
     * @param result First result
     * @param result2 Second result
     * @param result3 Third result
     */
    void process(VisualizerContext context, A result, B result2, C result3);
  }

  /**
   * Filtered iteration over a stacked hierarchy.
   *
   * This is really messy because the visualization hierarchy is typed Object.
   *
   * @param context Visualization context
   * @param clazz Type filter
   * @param <O> Object type
   * @return Iterator of results.
   */
  @SuppressWarnings("unchecked")
  public static <O extends VisualizationItem> Hierarchy.Iter<O> filter(VisualizerContext context, Class<? super O> clazz) {
    Hierarchy.Iter<Result> it1 = context.getHierarchy().iterAll();
    StackedIter<Object, Result> it2 = new StackedIter<>(it1, context.getVisHierarchy());
    if(!it2.valid()) {
      return HashMapHierarchy.emptyIterator();
    }
    return new FilteredIter<O>(it2, (Class<O>) clazz);
  }

  /**
   * Filtered iteration over a stacked hierarchy.
   *
   * This is really messy because the visualization hierarchy is typed Object.
   *
   * @param context Visualization context
   * @param start Starting object (in primary hierarchy!)
   * @param clazz Type filter
   * @param <O> Object type
   * @return Iterator of results.
   */
  @SuppressWarnings("unchecked")
  public static <O extends VisualizationItem> Hierarchy.Iter<O> filter(VisualizerContext context, Object start, Class<? super O> clazz) {
    if(start instanceof Result) { // In first hierarchy.
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendantsSelf((Result) start);
      StackedIter<Object, Result> it2 = new StackedIter<>(it1, context.getVisHierarchy());
      if(!it2.valid()) {
        return HashMapHierarchy.emptyIterator();
      }
      return new FilteredIter<O>(it2, (Class<O>) clazz);
    }
    Hierarchy.Iter<Object> it2 = context.getVisHierarchy().iterDescendants(start);
    if(!it2.valid()) {
      return HashMapHierarchy.emptyIterator();
    }
    return new FilteredIter<O>(it2, (Class<O>) clazz);
  }

  /**
   * Process new results.
   *
   * This is a bit painful, because we have two hierarchies with different
   * types: results, and visualizations.
   *
   * @param context Context
   * @param start Starting point
   * @param type1 First type
   * @param handler Handler
   */
  @SuppressWarnings("unchecked")
  public static <A> void findNew(VisualizerContext context, Object start, Class<? super A> type1, Handler1<A> handler) {
    final Hierarchy<Object> hier = context.getVisHierarchy();
    // Starting object:
    if(type1.isInstance(start)) {
      handler.process(context, (A) start);
    }
    // Children of start in first hierarchy:
    if(start instanceof Result) {
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendants((Result) start);
      for(; it1.valid(); it1.advance()) {
        final Result o1 = it1.get();
        if(!(type1.isInstance(o1))) {
          continue;
        }
        handler.process(context, (A) o1);
      }
    }
    // Children of start in second hierarchy:
    if(start instanceof VisualizationItem) {
      Iter<Object> it1 = hier.iterDescendants(start);
      for(; it1.valid(); it1.advance()) {
        final Object o1 = it1.get();
        if(!(type1.isInstance(o1))) {
          continue;
        }
        handler.process(context, (A) start);
      }
    }
  }

  /**
   * Process new result combinations of an object type1 (in first hierarchy) and
   * any child of type2 (in second hierarchy)
   *
   * This is a bit painful, because we have two hierarchies with different
   * types: results, and visualizations.
   *
   * @param context Context
   * @param start Starting point
   * @param type1 First type, in first hierarchy
   * @param type2 Second type, in second hierarchy
   * @param handler Handler
   */
  @SuppressWarnings("unchecked")
  public static <A extends Result, B extends VisualizationItem> void findNewSiblings(VisualizerContext context, Object start, Class<? super A> type1, Class<? super B> type2, Handler2<A, B> handler) {
    final Hierarchy<Object> vistree = context.getVisHierarchy();
    // Search start in first hierarchy:
    if(start instanceof Result) {
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendantsSelf((Result) start);
      for(; it1.valid(); it1.advance()) {
        final Result o1 = it1.get();
        if(!(type1.isInstance(o1))) {
          continue;
        }
        Iter<Object> it2 = vistree.iterDescendantsSelf(context.getBaseResult());
        for(; it2.valid(); it2.advance()) {
          final Object o2 = it2.get();
          if(!(type2.isInstance(o2))) {
            continue;
          }
          handler.process(context, (A) o1, (B) o2);
        }
      }
    }
    // Search start in second hierarchy:
    if(start instanceof VisualizationItem) {
      Iter<Object> it2 = vistree.iterDescendantsSelf(start);
      for(; it2.valid(); it2.advance()) {
        final Object o2 = it2.get();
        if(!(type2.isInstance(o2))) {
          continue;
        }
        Iter<Result> it1 = context.getHierarchy().iterAll();
        for(; it1.valid(); it1.advance()) {
          final Result o1 = it1.get();
          if(!(type1.isInstance(o1))) {
            continue;
          }
          handler.process(context, (A) o1, (B) o2);
        }
      }
    }
  }

  /**
   * Process new result combinations of an object type1 (in first hierarchy)
   * having a child of type2 (in second hierarchy).
   *
   * This is a bit painful, because we have two hierarchies with different
   * types: results, and visualizations.
   *
   * @param context Context
   * @param start Starting point
   * @param type1 First type, in first hierarchy
   * @param type2 Second type, in second hierarchy
   * @param handler Handler
   */
  @SuppressWarnings("unchecked")
  public static <A extends Result, B extends VisualizationItem> void findNewResultVis(VisualizerContext context, Object start, Class<? super A> type1, Class<? super B> type2, Handler2<A, B> handler) {
    final Hierarchy<Object> hier = context.getVisHierarchy();
    // Search start in first hierarchy:
    if(start instanceof Result) {
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendantsSelf((Result) start);
      for(; it1.valid(); it1.advance()) {
        final Result o1 = it1.get();
        if(!(type1.isInstance(o1))) {
          continue;
        }
        // Nasty: we now need to search backwards for crossover points:
        Iter<Result> it3 = context.getHierarchy().iterDescendantsSelf(o1);
        for(; it3.valid(); it3.advance()) {
          Iter<Object> it2 = hier.iterDescendantsSelf(it3.get());
          for(; it2.valid(); it2.advance()) {
            final Object o2 = it2.get();
            if(!(type2.isInstance(o2))) {
              continue;
            }
            handler.process(context, (A) o1, (B) o2);
          }
        }
      }
    }
    // Search start in second hierarchy:
    if(start instanceof VisualizationItem) {
      Iter<Object> it2 = hier.iterDescendantsSelf(start);
      for(; it2.valid(); it2.advance()) {
        final Object o2 = it2.get();
        if(!(type2.isInstance(o2))) {
          continue;
        }
        // Nasty: we now need to search backwards for crossover points:
        Iter<Object> it3 = hier.iterAncestorsSelf(start);
        for(; it3.valid(); it3.advance()) {
          if(!(it3.get() instanceof Result)) {
            continue;
          }
          // Now cross-over into primary hierarchy:
          Iter<Result> it1 = context.getHierarchy().iterAncestorsSelf((Result) it3.get());
          for(; it1.valid(); it1.advance()) {
            final Result o1 = it1.get();
            if(!(type1.isInstance(o1))) {
              continue;
            }
            handler.process(context, (A) o1, (B) o2);
          }
        }
      }
    }
  }

  /**
   * Utility function to change Visualizer visibility.
   *
   * @param context Visualization context
   * @param task Visualization task
   * @param visibility Visibility value
   */
  public static void setVisible(VisualizerContext context, VisualizationTask task, boolean visibility) {
    // Hide other tools
    if(visibility && task.tool) {
      Hierarchy<Object> vistree = context.getVisHierarchy();
      for(Hierarchy.Iter<?> iter2 = vistree.iterAll(); iter2.valid(); iter2.advance()) {
        if(!(iter2.get() instanceof VisualizationTask)) {
          continue;
        }
        VisualizationTask other = (VisualizationTask) iter2.get();
        if(other != task && other.tool && other.visible) {
          other.visible = false;
          context.visChanged(other);
        }
      }
    }
    task.visible = visibility;
    context.visChanged(task);
  }
}