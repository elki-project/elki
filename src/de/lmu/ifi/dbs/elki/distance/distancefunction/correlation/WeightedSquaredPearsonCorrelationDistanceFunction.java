package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Squared Pearson correlation distance function for feature vectors.
 * 
 * The squared Pearson correlation distance is computed from the Pearson
 * correlation coefficient <code>r</code> as: <code>1-r</code><sup>
 * <code>2</code></sup>. Hence, possible values of this distance are between 0
 * and 1.
 * 
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively or negatively correlated. For
 * Features with uncorrelated attributes, the distance value will be high (near
 * 1).
 * 
 * This variation is for weighted dimensions.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 */
public class WeightedSquaredPearsonCorrelationDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Weights
   */
  private double[] weights;

  /**
   * Provides a SquaredPearsonCorrelationDistanceFunction.
   * 
   * @param weights Weights
   */
  public WeightedSquaredPearsonCorrelationDistanceFunction(double[] weights) {
    super();
    this.weights = weights;
  }

  /**
   * Computes the squared Pearson correlation distance for two given feature
   * vectors.
   * 
   * The squared Pearson correlation distance is computed from the Pearson
   * correlation coefficient <code>r</code> as: <code>1-r</code><sup>
   * <code>2</code></sup>. Hence, possible values of this distance are between 0
   * and 1.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the squared Pearson correlation distance for two given feature
   *         vectors v1 and v2
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final double pcc = MathUtil.weightedPearsonCorrelationCoefficient(v1, v2, weights);
    return 1 - pcc * pcc;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return Arrays.equals(this.weights, ((WeightedSquaredPearsonCorrelationDistanceFunction)obj).weights);
  }
}