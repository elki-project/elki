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
package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Base interface used for projections in the ELKI visualizers.
 *
 * There are specialized interfaces for 1D and 2D that only compute the
 * projections in the required dimensions!
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @composed - - - LinearScale
 */
public interface Projection extends VisualizationItem {
  /**
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary#SCALE}.
   */
  double SCALE = StyleLibrary.SCALE;

  /**
   * Inverse scaling constant.
   */
  double INVSCALE = 1. / SCALE;

  /**
   * Get the input dimensionality of the projection.
   *
   * @return Input dimensionality
   */
  int getInputDimensionality();

  /**
   * Get the scale class for a particular dimension.
   *
   * @param d Dimension
   * @return Scale class
   */
  LinearScale getScale(int d);

  /**
   * Projector used for generating this projection.
   *
   * @return Projector
   */
  Projector getProjector();
}