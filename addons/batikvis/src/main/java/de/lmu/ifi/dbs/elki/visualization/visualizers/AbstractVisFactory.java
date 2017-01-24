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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Abstract superclass for Visualizers (aka: Visualization Factories).
 *
 * @author Remigius Wojdanowski
 * @since 0.4.0
 *
 * @apiviz.uses ThumbnailVisualization oneway - - «create»
 * @apiviz.excludeSubtypes
 */
public abstract class AbstractVisFactory implements VisFactory {
  /**
   * Constructor.
   */
  protected AbstractVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj, int thumbsize) {
    if(width <= 0 || height <= 0) {
      LoggingUtil.warning("Cannot generate visualization of 0 size.", new Throwable());
      return null;
    }
    if(allowThumbnails(task)) {
      return new ThumbnailVisualization(this, task, plot, width, height, proj, thumbsize);
    }
    return makeVisualization(task, plot, width, height, proj);
  }

  @Override
  abstract public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj);

  /**
   * Test whether to do a thumbnail or a full rendering.
   *
   * Override this with "false" to disable thumbnails!
   *
   * @param task Task requested
   */
  public boolean allowThumbnails(VisualizationTask task) {
    return true;
  }
}