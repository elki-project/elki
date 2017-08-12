/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.PearsonCorrelation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

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
 * @since 0.3
 */
public class WeightedSquaredPearsonCorrelationDistanceFunction extends AbstractNumberVectorDistanceFunction implements WeightedNumberVectorDistanceFunction<NumberVector> {
  /**
   * Weights
   */
  private double[] weights;

  /**
   * Constructor.
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
  public double distance(NumberVector v1, NumberVector v2) {
    final double pcc = PearsonCorrelation.weightedCoefficient(v1, v2, weights);
    return 1 - pcc * pcc;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof WeightedSquaredPearsonCorrelationDistanceFunction && //
        Arrays.equals(this.weights, ((WeightedSquaredPearsonCorrelationDistanceFunction) obj).weights));
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Weight array
     */
    protected double[] weights;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter weightsP = new DoubleListParameter(WEIGHTS_ID);
      if(config.grab(weightsP)) {
        weights = weightsP.getValue().clone();
      }
    }

    @Override
    protected WeightedSquaredPearsonCorrelationDistanceFunction makeInstance() {
      return new WeightedSquaredPearsonCorrelationDistanceFunction(weights);
    }
  }
}