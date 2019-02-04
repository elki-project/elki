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
package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;

/**
 * Static visualization
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class StaticVisualizationInstance extends AbstractVisualization {
  /**
   * Unchanging precomputed visualization.
   *
   * @param context Visualizer context
   * @param task Task to visualize
   * @param plot Plot to draw to
   * @param width Embedding width
   * @param height Embedding height
   * @param element Element containing the resulting visualization
   */
  public StaticVisualizationInstance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Element element) {
    super(context, task, plot, width, height);
    this.layer = element;
  }

  @Override
  public void incrementalRedraw() {
    // Do nothing - we keep our static layer
  }

  @Override
  public void fullRedraw() {
    // Do nothing - we keep our static layer
  }
}
