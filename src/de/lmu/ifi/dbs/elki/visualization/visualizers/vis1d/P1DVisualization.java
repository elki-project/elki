package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

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

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;

/**
 * One-dimensional projected visualization.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Projection1D
 */
public abstract class P1DVisualization extends AbstractVisualization {
  /**
   * The current projection
   */
  final protected Projection1D proj;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public P1DVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
  }
}