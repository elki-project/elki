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
package de.lmu.ifi.dbs.elki.visualization.parallel3d.layout;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.DependenceMeasure;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Similarity based layouting algorithms.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface SimilarityBasedLayouter3DPC extends Layouter3DPC<NumberVector> {
  /**
   * Option for similarity measure.
   */
  OptionID SIM_ID = new OptionID("parallel3d.sim", "Similarity measure for spanning tree.");

  /**
   * Get the similarity measure to use.
   * 
   * @return Similarity measure.
   */
  DependenceMeasure getSimilarity();

  /**
   * Main analysis method.
   * 
   * @param dim Dimensionality
   * @param mat Similarity matrix
   * @return Layout
   */
  Layout layout(final int dim, double[] mat);
}
