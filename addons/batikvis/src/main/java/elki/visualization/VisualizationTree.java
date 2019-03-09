/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.visualization;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import elki.result.Metadata;
import elki.utilities.datastructures.hierarchy.HashMapHierarchy;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.hierarchy.StackedIter;
import elki.utilities.datastructures.iterator.It;

/**
 * Tree - actually a forest - to manage visualizations.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Handler1
 * @has - - - Handler2
 * @has - - - Handler3
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
      final VisualizationListener listener = vlistenerList.get(i);
      if(listener != null) {
        listener.visualizationChanged(item);
      }
    }
  }

  /**
   * Filtered iteration over a stacked hierarchy.
   * <p>
   * This is really messy because the visualization hierarchy is typed Object.
   *
   * @param context Visualization context
   * @return Iterator of results.
   */
  public static It<Object> findVis(VisualizerContext context) {
    return new StackedIter<>(Metadata.hierarchyOf(context.getBaseResult()).iterDescendantsSelf(), context.getVisHierarchy());
  }

  /**
   * Filtered iteration over a stacked hierarchy.
   * <p>
   * This is really messy because the visualization hierarchy is typed Object.
   *
   * @param context Visualization context
   * @param start Starting object (in primary hierarchy!)
   * @return Iterator of results.
   */
  public static It<Object> findVis(VisualizerContext context, Object start) {
    Metadata.Hierarchy hier = Metadata.hierarchyOf(start);
    if(hier.numChildren() > 0) {
      return new StackedIter<>(hier.iterDescendantsSelf(), context.getVisHierarchy());
    }
    return context.getVisHierarchy().iterDescendantsSelf(start);
  }

  /**
   * Iterate over the <em>primary result tree</em>.
   *
   * @param context Visualization context
   * @param start Starting object (in primary hierarchy!)
   * @return Iterator of results.
   */
  public static It<Object> findNewResults(VisualizerContext context, Object start) {
    return Metadata.hierarchyOf(start).iterDescendantsSelf();
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
  public static <A, B extends VisualizationItem> void findNewSiblings(VisualizerContext context, Object start, Class<? super A> type1, Class<? super B> type2, BiConsumer<A, B> handler) {
    // Search start in first hierarchy:
    final Metadata.Hierarchy hier = Metadata.hierarchyOf(context.getBaseResult());
    final Hierarchy<Object> vistree = context.getVisHierarchy();
    // New result:
    for(It<A> it1 = Metadata.hierarchyOf(start).iterDescendantsSelf().filter(type1); it1.valid(); it1.advance()) {
      final A result = it1.get();
      // Existing visualization:
      for(It<B> it2 = vistree.iterDescendantsSelf(context.getBaseResult()).filter(type2); it2.valid(); it2.advance()) {
        handler.accept(result, it2.get());
      }
    }
    // New visualization:
    for(It<B> it2 = vistree.iterDescendantsSelf(start).filter(type2); it2.valid(); it2.advance()) {
      final B vis = it2.get();
      // Existing result:
      for(It<A> it1 = hier.iterDescendantsSelf().filter(type1); it1.valid(); it1.advance()) {
        handler.accept(it1.get(), vis);
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
  public static <A, B extends VisualizationItem> void findNewResultVis(VisualizerContext context, Object start, Class<? super A> type1, Class<? super B> type2, BiConsumer<A, B> handler) {
    final Hierarchy<Object> hier = context.getVisHierarchy();
    // Search start in first hierarchy:
    for(It<A> it1 = Metadata.hierarchyOf(start).iterDescendantsSelf().filter(type1); it1.valid(); it1.advance()) {
      final A result = it1.get();
      // Find descendant results in result hierarchy:
      for(It<Object> it3 = Metadata.hierarchyOf(result).iterDescendantsSelf(); it3.valid(); it3.advance()) {
        // Find descendant in visualization hierarchy:
        for(It<B> it2 = hier.iterDescendantsSelf(it3.get()).filter(type2); it2.valid(); it2.advance()) {
          handler.accept(result, it2.get());
        }
      }
    }
    // Search start in second hierarchy:
    if(start instanceof VisualizationItem) {
      for(It<B> it2 = hier.iterDescendantsSelf(start).filter(type2); it2.valid(); it2.advance()) {
        final B vis = it2.get();
        // Find ancestor result in visualization hierarchy:
        for(It<Object> it3 = hier.iterAncestorsSelf(vis); it3.valid(); it3.advance()) {
          // Find ancestor in result hierarchy:
          for(It<A> it1 = Metadata.hierarchyOf(it3.get()).iterAncestorsSelf().filter(type1); it1.valid(); it1.advance()) {
            handler.accept(it1.get(), vis);
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
    if(visibility && task.isTool()) {
      Hierarchy<Object> vistree = context.getVisHierarchy();
      for(It<VisualizationTask> iter2 = vistree.iterAll().filter(VisualizationTask.class); iter2.valid(); iter2.advance()) {
        VisualizationTask other = iter2.get();
        if(other != task && other.isTool() && other.isVisible()) {
          context.visChanged(other.visibility(false));
        }
      }
    }
    context.visChanged(task.visibility(visibility));
  }
}
