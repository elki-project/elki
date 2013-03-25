package de.lmu.ifi.dbs.elki.visualization.visualizers;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.result.ResultUtil;
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
   */
  private VisualizerUtil() {
    // Do not instantiate.
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
    if (visibility && task.tool) {
      final List<VisualizationTask> visualizers = ResultUtil.filterResults(context.getResult(), VisualizationTask.class);
      for (VisualizationTask other : visualizers) {
        if (other != task && other.tool && other.visible) {
          other.visible = false;
          context.getHierarchy().resultChanged(other);
        }
      }
    }
    task.visible = visibility;
    context.getHierarchy().resultChanged(task);
  };
}
