package de.lmu.ifi.dbs.elki.visualization.gui.overview;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Manage the Overview plot canvas.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf PlotItem
 */
class PlotMap extends HashMap<DoubleDoublePair, PlotItem> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * X coordinates seen
   */
  DoubleMinMax minmaxx = new DoubleMinMax();

  /**
   * Y coordinates seen
   */
  DoubleMinMax minmaxy = new DoubleMinMax();

  /**
   * Constructor.
   */
  PlotMap() {
    super();
  }

  /**
   * Place a new visualization on the chart.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @param w Width
   * @param h Height
   * @param v Visualization
   */
  void addVis(double x, double y, double w, double h, Projection proj, VisualizationTask v) {
    final DoubleDoublePair pos = new DoubleDoublePair(x, y);
    PlotItem l = this.get(pos);
    if(l == null) {
      l = new PlotItem(x, y, w, h, proj);
      this.put(pos, l);
    }
    else {
      // Sanity check
      if(l.w != w || l.h != h) {
        LoggingUtil.warning("Layout error - different object sizes at the same map position!");
      }
      if(l.proj != proj) {
        LoggingUtil.warning("Layout error - two different projections used at the same map position.");
      }
    }
    l.add(v);
    // Update min/max
    minmaxx.put(x);
    minmaxx.put(x + w);
    minmaxy.put(y);
    minmaxy.put(y + h);
  }

  /**
   * Get the visualization on the given coordinates.
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return Visualizations at this position.
   */
  PlotItem get(double x, double y) {
    return this.get(new DoubleDoublePair(x, y));
  }

  /**
   * Get width in plot units
   * 
   * @return width
   */
  public double getWidth() {
    return minmaxx.getMax() - minmaxx.getMin();
  }

  /**
   * Get height in plot units.
   * 
   * @return height
   */
  public double getHeight() {
    return minmaxy.getMax() - minmaxy.getMin();
  }
}