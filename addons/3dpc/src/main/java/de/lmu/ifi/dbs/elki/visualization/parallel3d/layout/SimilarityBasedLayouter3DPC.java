package de.lmu.ifi.dbs.elki.visualization.parallel3d.layout;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Similarity based layouting algorithms.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Data type
 */
public interface SimilarityBasedLayouter3DPC<V> extends Layouter3DPC<V> {
  /**
   * Option for similarity measure.
   */
  public static final OptionID SIM_ID = new OptionID("parallel3d.sim", "Similarity measure for spanning tree.");

  /**
   * Get the similarity measure to use.
   * 
   * @return Similarity measure.
   */
  DimensionSimilarity<? super V> getSimilarity();

  /**
   * Main analysis method.
   * 
   * @param dim Dimensionality
   * @param mat Similarity matrix
   * @return Layout
   */
  Layout layout(final int dim, DimensionSimilarityMatrix mat);
}
