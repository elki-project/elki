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
package de.lmu.ifi.dbs.elki.visualization.visualizers.histogram;

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;

/**
 * One-dimensional projected visualization.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @has - - - Projection1D
 */
public abstract class AbstractHistogramVisualization extends AbstractVisualization {
  /**
   * The current projection
   */
  final protected Projection1D proj;

  /**
   * Constructor.
   *
   * @param context Visualizer context
   * @param task Visualization task
   * @param plot Plot to draw to
   * @param width Embedding width
   * @param height Embedding height
   * @param proj Projection
   */
  public AbstractHistogramVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    super(context, task, plot, width, height);
    assert(proj instanceof Projection1D) : "Visualizer attached to wrong projection!";
    this.proj = (Projection1D) proj;
  }

  @Override
  public void resultChanged(Result current) {
    super.resultChanged(current);
    if(proj != null && current == proj) {
      svgp.requestRedraw(this.task, this);
      return;
    }
  }
}