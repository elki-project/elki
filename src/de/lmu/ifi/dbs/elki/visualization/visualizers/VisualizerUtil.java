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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.AbstractFilteredIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
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
   * Find the visualizer context in a result tree.
   * 
   * @param baseResult base result to start searching at.
   * @return Visualizer context
   */
  public static VisualizerContext getContext(HierarchicalResult baseResult) {
    IterableIterator<VisualizerContext> iter = ResultUtil.filteredResults(baseResult, VisualizerContext.class);
    if (iter.hasNext()) {
      return iter.next();
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
    // Currently enabled?
    Boolean enabled = task.getGenerics(VisualizationTask.META_VISIBLE, Boolean.class);
    if(enabled == null) {
      enabled = task.getGenerics(VisualizationTask.META_VISIBLE_DEFAULT, Boolean.class);
    }
    if(enabled == null) {
      enabled = true;
    }
    return enabled;
  }

  /**
   * Utility function to test for a visualizer being a "tool".
   * 
   * @param vis Visualizer to test
   * @return true for a tool
   */
  public static boolean isTool(VisualizationTask vis) {
    // Currently enabled?
    Boolean tool = vis.getGenerics(VisualizationTask.META_TOOL, Boolean.class);
    return (tool != null) && tool;
  }

  /**
   * Utility function to test for a visualizer having options.
   * 
   * @param vis Visualizer to test
   * @return true when it has options
   */
  public static boolean hasOptions(VisualizationTask vis) {
    // Currently enabled?
    Boolean hasoptions = vis.getGenerics(VisualizationTask.META_HAS_OPTIONS, Boolean.class);
    return (hasoptions != null) && hasoptions;
  }

  /**
   * Filter for number vector field representations
   * 
   * @param result Result to filter
   * @return Iterator over suitable representations
   */
  // TODO: move to DatabaseUtil?
  public static Iterator<Relation<? extends NumberVector<?, ?>>> iterateVectorFieldRepresentations(final Result result) {
    final Iterator<Relation<?>> parent = ResultUtil.filteredResults(result, Relation.class);
    return new AbstractFilteredIterator<Relation<?>, Relation<? extends NumberVector<?, ?>>>() {
      @Override
      protected Iterator<Relation<?>> getParentIterator() {
        return parent;
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Relation<? extends NumberVector<?, ?>> testFilter(Relation<?> nextobj) {
        final SimpleTypeInformation<?> type = nextobj.getDataTypeInformation();
        if(!NumberVector.class.isAssignableFrom(type.getRestrictionClass())) {
          return null;
        }
        if(!(type instanceof VectorFieldTypeInformation)) {
          return null;
        }
        return (Relation<? extends NumberVector<?, ?>>) nextobj;
      }
    };
  }

  /**
   * Test whether a thumbnail is enabled for this visualizer.
   * 
   * @param vis Visualizer
   * @return boolean
   */
  public static boolean thumbnailEnabled(VisualizationTask vis) {
    Boolean nothumb = vis.getGenerics(VisualizationTask.META_NOTHUMB, Boolean.class);
    return (nothumb == null) || !nothumb;
  }

  /**
   * Test whether a detail plot is available for this task.
   * 
   * @param vis Task
   * @return boolean
   */
  public static boolean detailsEnabled(VisualizationTask vis) {
    Boolean nodetail = vis.getGenerics(VisualizationTask.META_NODETAIL, Boolean.class);
    return (nodetail == null) || !nodetail;
  }
}