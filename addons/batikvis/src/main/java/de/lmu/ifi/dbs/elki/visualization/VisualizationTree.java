/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.visualization;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.FilteredIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HashMapHierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.StackedIter;

/**
 * Tree - actually a forest - to manage visualizations.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.has Handler1
 * @apiviz.has Handler2
 * @apiviz.has Handler3
 */
public class VisualizationTree extends HashMapHierarchy<Object> {
  /**
   * The event listeners for this context.
   */
  private ArrayList<VisualizationListener> vlistenerList = new ArrayList<>();

  /**
   * Constructor.
   */
  public VisualizationTree() {
    super();
  }

  /**
   * Add a listener.
   *
   * @param listener Listener to add
   */
  public void addVisualizationListener(VisualizationListener listener) {
    for(int i = 0; i < vlistenerList.size(); i++) {
      if(vlistenerList.get(i) == listener) {
        return;
      }
    }
    vlistenerList.add(listener);
  }

  /**
   * Add a listener.
   *
   * @param listener Listener to remove
   */
  public void removeVisualizationListener(VisualizationListener listener) {
    vlistenerList.remove(listener);
  }

  /**
   * A visualization item has changed.
   *
   * @param item Item that has changed
   */
  public void visChanged(VisualizationItem item) {
    for(int i = vlistenerList.size(); --i >= 0;) {
      vlistenerList.get(i).visualizationChanged(item);
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
    Hierarchy.Iter<Object> it2 = context.getVisHierarchy().iterDescendantsSelf(start);
    if(!it2.valid()) {
      return HashMapHierarchy.emptyIterator();
    }
    return new FilteredIter<O>(it2, (Class<O>) clazz);
  }

  /**
   * Filtered iteration over the primary result tree.
   *
   * @param context Visualization context
   * @param start Starting object (in primary hierarchy!)
   * @param clazz Type filter
   * @param <O> Result type type
   * @return Iterator of results.
   */
  @SuppressWarnings("unchecked")
  public static <O extends Result> Hierarchy.Iter<O> filterResults(VisualizerContext context, Object start, Class<? super O> clazz) {
    if(start instanceof Result) { // In first hierarchy.
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendantsSelf((Result) start);
      return new FilteredIter<O>(it1, (Class<O>) clazz);
    }
    return HashMapHierarchy.emptyIterator();
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
    // Children of start in first hierarchy:
    if(start instanceof Result) {
      Hierarchy.Iter<Result> it1 = context.getHierarchy().iterDescendantsSelf((Result) start);
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
      Iter<Object> it1 = hier.iterDescendantsSelf(start);
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