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

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultProcessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;

/**
 * Defines the requirements for a visualizer. <br>
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor) to be used for parameterization.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses Visualization - - «create»
 * @apiviz.uses VisualizationTask - - «create»
 */
public interface VisFactory extends ResultProcessor, Parameterizable {
  /**
   * Add visualizers for the given result (tree) to the context.
   * 
   * @param baseResult Context to work with
   * @param newResult Result to process
   */
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult);

  /**
   * Produce a visualization instance for the given task
   * 
   * @param task Visualization task
   * @return Visualization
   */
  public Visualization makeVisualization(VisualizationTask task);

  /**
   * Produce a visualization instance for the given task that may use thumbnails
   * 
   * @param task Visualization task
   * @return Visualization
   */
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task);
}