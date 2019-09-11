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
package elki.distance;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Weighted Canberra distance function, a variation of Manhattan distance.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class WeightedCanberraDistance extends AbstractNumberVectorDistance implements SpatialPrimitiveDistance<NumberVector>, WeightedNumberVectorDistance<NumberVector> {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   */
  public WeightedCanberraDistance(double[] weights) {
    super();
    this.weights = weights;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2, weights.length);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double div = Math.abs(xd) + Math.abs(yd);
      if(div > 0.) {
        agg += weights[d] * Math.abs(xd - yd) / div;
      }
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2, weights.length);
    double agg = 0.0;
    for(int d = 0; d < dim; d++) {
      final double diff;
      if(mbr1.getMax(d) < mbr2.getMin(d)) {
        diff = mbr2.getMin(d) - mbr1.getMax(d);
      }
      else if(mbr1.getMin(d) > mbr2.getMax(d)) {
        diff = mbr1.getMin(d) - mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        continue;
      }
      final double a1 = Math.max(-mbr1.getMin(d), mbr1.getMax(d));
      final double a2 = Math.max(-mbr2.getMin(d), mbr2.getMax(d));
      final double div = a1 + a2;
      // Cannot be 0, because then diff = 0, and we continued before.
      agg += weights[d] * diff / div;
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    // As this is also reffered to as "canberra metric", it is probably a metric
    // But *maybe* only for positive numbers only?
    return true;
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
    public WeightedCanberraDistance make() {
      return new WeightedCanberraDistance(weights);
    }
  }
}
