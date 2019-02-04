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
package de.lmu.ifi.dbs.elki.visualization.gui;

import java.util.concurrent.ConcurrentLinkedDeque;

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * SVG plot that allows visualization to schedule updates.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class VisualizationPlot extends SVGPlot {
  /**
   * Pending redraw request in Batik.
   */
  protected Runnable pendingRedraw = null;

  /**
   * Update queue.
   */
  protected ConcurrentLinkedDeque<Visualization> updateQueue = new ConcurrentLinkedDeque<>();

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final void synchronizedRedraw() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(VisualizationPlot.this.pendingRedraw == this) {
          VisualizationPlot.this.pendingRedraw = null;
          VisualizationPlot.this.redraw();
        }
      }
    };
    pendingRedraw = pr;
    scheduleUpdate(pr);
  }

  /**
   * Redraw all pending updates.
   */
  protected void redraw() {
    while(!updateQueue.isEmpty()) {
      Visualization vis = updateQueue.pop();
      vis.incrementalRedraw();
    }
  }

  /**
   * Request a redraw of a visualization.
   */
  public void requestRedraw(VisualizationTask task, Visualization vis) {
    updateQueue.add(vis);
    synchronizedRedraw();
  }
}
