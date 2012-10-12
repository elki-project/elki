package de.lmu.ifi.dbs.elki.visualization.visualizers;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.AbstractFilteredIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Visualizer utilities.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses VisualizationTask - - inspects
 */
public final class VisualizerUtil {
  /**
   * Fake constructor: do not instantiate.
   * 
   */
  private VisualizerUtil() {
    // Do not instantiate.
  }

  /**
   * Find the visualizer context in a result tree.
   * 
   * @param baseResult base result to start searching at.
   * @return Visualizer context
   */
  public static VisualizerContext getContext(HierarchicalResult baseResult) {
    List<VisualizerContext> contexts = ResultUtil.filterResults(baseResult, VisualizerContext.class);
    if (!contexts.isEmpty()) {
      return contexts.get(0);
    } else {
      return null;
    }
  }

  /**
   * Utility function to test for Visualizer visibility.
   * 
   * @param task Visualization task
   * @return true when visible
   */
  public static boolean isVisible(VisualizationTask task) {
    return task.visible;
  }

  /**
   * Utility function to change Visualizer visibility.
   * 
   * @param task Visualization task
   * @param visibility Visibility value
   */
  public static void setVisible(VisualizationTask task, boolean visibility) {
    VisualizerContext context = task.getContext();
    if (context != null) {
      setVisible(context, task, visibility);
    } else {
      LoggingUtil.warning("setVisible called without context in task.", new Throwable());
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
    if (visibility && VisualizerUtil.isTool(task)) {
      final List<VisualizationTask> visualizers = ResultUtil.filterResults(context.getResult(), VisualizationTask.class);
      for (VisualizationTask other : visualizers) {
        if (other != task && VisualizerUtil.isTool(other) && VisualizerUtil.isVisible(other)) {
          other.visible = false;
          context.getHierarchy().resultChanged(other);
        }
      }
    }
    task.visible = visibility;
    context.getHierarchy().resultChanged(task);
  }

  /**
   * Utility function to test for a visualizer being a "tool".
   * 
   * @param vis Visualizer to test
   * @return true for a tool
   */
  public static boolean isTool(VisualizationTask vis) {
    return vis.tool;
  }

  /**
   * Utility function to test for a visualizer being "no export".
   * 
   * @param vis Visualizer to test
   * @return true when not to export
   */
  public static boolean isNoExport(VisualizationTask vis) {
    return vis.noexport;
  }

  /**
   * Utility function to test for a visualizer having options.
   * 
   * @param vis Visualizer to test
   * @return true when it has options
   */
  public static boolean hasOptions(VisualizationTask vis) {
    return vis.hasoptions;
  }

  /**
   * Filter for number vector field representations.
   * 
   * @param result Result to filter
   * @return Iterator over suitable representations
   */
  // TODO: move to DatabaseUtil?
  public static Iterator<Relation<? extends NumberVector<?>>> iterateVectorFieldRepresentations(final Result result) {
    List<Relation<?>> parent = ResultUtil.filterResults(result, Relation.class);
    return new VectorspaceIterator(parent.iterator());
  }

  /**
   * Iterate over vectorspace.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static class VectorspaceIterator extends AbstractFilteredIterator<Relation<?>, Relation<? extends NumberVector<?>>> {
    /**
     * Parent iterator.
     */
    private Iterator<Relation<?>> parent;

    /**
     * Constructor.
     * 
     * @param parent Parent iterator
     */
    public VectorspaceIterator(Iterator<Relation<?>> parent) {
      super();
      this.parent = parent;
    }

    @Override
    protected Iterator<Relation<?>> getParentIterator() {
      return parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Relation<? extends NumberVector<?>> testFilter(Relation<?> nextobj) {
      final SimpleTypeInformation<?> type = nextobj.getDataTypeInformation();
      if (!NumberVector.class.isAssignableFrom(type.getRestrictionClass())) {
        return null;
      }
      if (!(type instanceof VectorFieldTypeInformation)) {
        return null;
      }
      return (Relation<? extends NumberVector<?>>) nextobj;
    }
  };

  /**
   * Test whether a thumbnail is enabled for this visualizer.
   * 
   * @param vis Visualizer
   * @return boolean
   */
  public static boolean thumbnailEnabled(VisualizationTask vis) {
    return vis.thumbnail;
  }

  /**
   * Test whether a detail plot is available for this task.
   * 
   * @param vis Task
   * @return boolean
   */
  public static boolean detailsEnabled(VisualizationTask vis) {
    return !vis.nodetail;
  }
}
