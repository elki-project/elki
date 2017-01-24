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
package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProcessor;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Defines the requirements for a visualizer. <br>
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor) to be used for parameterization.
 *
 * @author Remigius Wojdanowski
 * @since 0.3
 *
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses Visualization - - «create»
 * @apiviz.uses VisualizationTask - - «create»
 */
public interface VisFactory extends VisualizationProcessor {
  /**
   * Add visualizers for the given result (tree) to the context.
   *
   * @param context Visualization context
   * @param start Result to process
   */
  @Override
  void processNewResult(VisualizerContext context, Object start);

  /**
   * Produce a visualization instance for the given task
   *
   * @param task Visualization task
   * @param plot Plot
   * @param width Width
   * @param height Height
   * @param proj Projection
   * @return Visualization
   */
  Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj);

  /**
   * Produce a visualization instance for the given task that may use thumbnails
   *
   * @param task Visualization task
   * @param plot Plot
   * @param width Width
   * @param height Height
   * @param proj Projection
   * @param thumbsize Thumbnail size
   * @return Visualization
   */
  Visualization makeVisualizationOrThumbnail(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj, int thumbsize);
}