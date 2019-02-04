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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;

/**
 * A projector is responsible for adding projections to the visualization.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface Projector extends VisualizationItem {
  /**
   * Produce an arrangement of projections.
   *
   * @param context Visualization context
   * @return Arrangement.
   */
  Collection<PlotItem> arrange(VisualizerContext context);
}