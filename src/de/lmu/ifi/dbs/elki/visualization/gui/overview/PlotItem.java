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

import java.util.LinkedList;

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Item to collect visualization tasks on a specific position on the plot map.
 * 
 * Note: this is a {@code LinkedList<VisualizationTask>}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Projection
 */
public class PlotItem extends LinkedList<VisualizationTask> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Position: x
   */
  public final double x;

  /**
   * Position: y
   */
  public final double y;

  /**
   * Size: width
   */
  public final double w;

  /**
   * Size: height
   */
  public final double h;

  /**
   * Projection (may be {@code null}!)
   */
  public final Projection proj;
  
  /**
   * Constructor.
   * 
   * @param x Position: x
   * @param y Position: y
   * @param w Position: w
   * @param h Position: h
   * @param proj Projection
   */
  public PlotItem(double x, double y, double w, double h, Projection proj) {
    super();
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.proj = proj;
  }

  @Override
  public int hashCode() {
    // We can't have our hashcode change with the list contents!
    return System.identityHashCode(this);
  }
}