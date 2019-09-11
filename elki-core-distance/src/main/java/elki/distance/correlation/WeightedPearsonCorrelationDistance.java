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
package elki.distance.correlation;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.distance.WeightedNumberVectorDistance;
import elki.math.PearsonCorrelation;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Pearson correlation distance function for feature vectors.
 * 
 * The Pearson correlation distance is computed from the Pearson correlation
 * coefficient <code>r</code> as: <code>1-r</code>. Hence, possible values of
 * this distance are between 0 and 2.
 * 
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively correlated, it will be high
 * (near 2), if their attribute values are dimension-wise strictly negatively
 * correlated. For Features with uncorrelated attributes, the distance value
 * will be intermediate (around 1).
 * 
 * This variation is for weighted dimensions.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.4.0
 */
public class WeightedPearsonCorrelationDistance extends AbstractNumberVectorDistance implements WeightedNumberVectorDistance<NumberVector> {
  /**
   * Weights
   */
  private double[] weights;

  /**
   * Constructor.
   * 
   * @param weights Weights
   */
  public WeightedPearsonCorrelationDistance(double[] weights) {
    super();
    this.weights = weights;
  }

  /**
   * Computes the Pearson correlation distance for two given feature vectors.
   * 
   * The Pearson correlation distance is computed from the Pearson correlation
   * coefficient <code>r</code> as: <code>1-r</code>. Hence, possible values of
   * this distance are between 0 and 2.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Pearson correlation distance for two given feature vectors v1
   *         and v2
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return 1 - PearsonCorrelation.weightedCoefficient(v1, v2, weights);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj != null && this.getClass().equals(obj.getClass()) && //
        Arrays.equals(this.weights, ((WeightedPearsonCorrelationDistance) obj).weights));
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public String toString() {
    return "WeightedPearsonCorrelationDistance";
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Weight array
     */
    protected double[] weights;

    @Override
    public void configure(Parameterization config) {
      new DoubleListParameter(WEIGHTS_ID) //
          .grab(config, x -> weights = x.clone());
    }

    @Override
    public WeightedPearsonCorrelationDistance make() {
      return new WeightedPearsonCorrelationDistance(weights);
    }
  }
}
