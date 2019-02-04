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
package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.Predicate;

/**
 * Class that uses a NumberVector as reference, and considers all non-zero
 * values as positive entries.
 * 
 * @composed - - - NumberVector
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class VectorOverThreshold implements Predicate<AbstractVectorIter> {
  /**
   * Vector to use as reference
   */
  NumberVector vec;

  /**
   * Threshold
   */
  double threshold;

  /**
   * Number of positive values.
   */
  int numpos;

  /**
   * Constructor.
   * 
   * @param vec Reference vector.
   * @param threshold Threshold value.
   */
  public VectorOverThreshold(NumberVector vec, double threshold) {
    super();
    this.vec = vec;
    this.threshold = threshold;
    this.numpos = 0;
    for(int i = 0, l = vec.getDimensionality(); i < l; i++) {
      if(vec.doubleValue(i) > threshold) {
        ++numpos;
      }
    }
  }

  @Override
  public boolean test(AbstractVectorIter o) {
    return vec.doubleValue(o.dim()) > threshold;
  }

  @Override
  public int numPositive() {
    return numpos;
  }
  
  /**
   * Dimensionality of the underlying truth vector (positive and negative values).
   *
   * @return Dimensionality
   */
  public int getDimensionality() {
    return vec.getDimensionality();
  }
}