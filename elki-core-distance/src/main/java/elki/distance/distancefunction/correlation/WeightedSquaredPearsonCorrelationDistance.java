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
package elki.distance.distancefunction.correlation;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.distance.distancefunction.AbstractNumberVectorDistance;
import elki.distance.distancefunction.WeightedNumberVectorDistance;
import elki.math.PearsonCorrelation;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Weighted squared Pearson correlation distance function for feature vectors.
 * <p>
 * The squared Pearson correlation distance is computed from the
 * Pearson correlation coefficient \(r\) as: \(1-r^2\).
 * Hence, possible values of this distance are between 0 and 1.
 * <p>
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively or negatively correlated.
 * For features with uncorrelated attributes, the distance value will be high
 * (near 1).
 * <p>
 * This variation is for weighted dimensions.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.4.0
 */
public class WeightedSquaredPearsonCorrelationDistance extends AbstractNumberVectorDistance implements WeightedNumberVectorDistance<NumberVector> {
  /**
   * Weights
   */
  private double[] weights;

  /**
   * Constructor.
   * 
   * @param weights Weights
   */
  public WeightedSquaredPearsonCorrelationDistance(double[] weights) {
    super();
    this.weights = weights;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final double pcc = PearsonCorrelation.weightedCoefficient(v1, v2, weights);
    return 1 - pcc * pcc;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj != null && this.getClass().equals(obj.getClass()) && //
        Arrays.equals(this.weights, ((WeightedSquaredPearsonCorrelationDistance) obj).weights));
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
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
    protected WeightedSquaredPearsonCorrelationDistance makeInstance() {
      return new WeightedSquaredPearsonCorrelationDistance(weights);
    }
  }
}
