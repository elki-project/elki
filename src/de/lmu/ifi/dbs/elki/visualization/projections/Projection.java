package de.lmu.ifi.dbs.elki.visualization.projections;

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

import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Base interface used for projections in the ELKI visualizers.
 * 
 * There are specialized interfaces for 1D and 2D that only compute the
 * projections in the required dimensions!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.composedOf LinearScale
 */
public interface Projection extends HierarchicalResult {
  /**
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary#SCALE}.
   */
  public static final double SCALE = StyleLibrary.SCALE;
  
  /**
   * Get the input dimensionality of the projection.
   * 
   * @return Input dimensionality
   */
  public int getInputDimensionality();

  /**
   * Get the scale class for a particular dimension.
   * 
   * @param d Dimension
   * @return Scale class
   */
  public LinearScale getScale(int d);
}