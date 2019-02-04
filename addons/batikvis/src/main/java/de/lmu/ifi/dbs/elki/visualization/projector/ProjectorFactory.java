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
package de.lmu.ifi.dbs.elki.visualization.projector;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProcessor;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * A projector is responsible for adding projections to the visualization by
 * detecting appropriate relations in the database.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Projector
 */
public interface ProjectorFactory extends VisualizationProcessor {
  /**
   * Add projections for the given result (tree) to the result tree.
   * @param context Visualization context
   * @param start Result to process
   */
  @Override
  void processNewResult(VisualizerContext context, Object start);
}