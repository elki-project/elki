/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.linearalgebra.pca.filter;

import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The "drop" filter looks for the largest drop in normalized relative
 * eigenvalues.
 * <p>
 * Let \( s_1 \ldots s_n \) be the eigenvalues.
 * <p>
 * Let \( a_k := 1/(n-k) \sum_{i=k..n} s_i \)
 * <p>
 * Then \( r_k := s_k / a_k \) is the relative eigenvalue.
 * <p>
 * The drop filter searches for \(\operatorname{arg\,max}_k r_k / r_{k+1} \)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Title("Drop EigenPair Filter")
public class DropEigenPairFilter implements EigenPairFilter {
  /**
   * The default value for walpha. Not used by default, we're going for maximum
   * contrast only.
   */
  public static final double DEFAULT_WALPHA = 0.0;

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double walpha = 0.0;

  /**
   * Constructor.
   *
   * @param walpha
   */
  public DropEigenPairFilter(double walpha) {
    super();
    this.walpha = walpha;
  }

  @Override
  public int filter(double[] eigenValues) {
    int contrastMaximum = eigenValues.length;
    double maxContrast = 0.0;

    // calc the eigenvalue sum.
    double eigenValueSum = 0.0;
    for(int i = 0; i < eigenValues.length; i++) {
      eigenValueSum += eigenValues[i];
    }
    // Minimum value
    final double weakEigenvalue = walpha * eigenValueSum / eigenValues.length;
    // Now find the maximum contrast, scanning backwards.
    double prev_sum = eigenValues[eigenValues.length - 1];
    double prev_rel = 1.0;
    for(int i = eigenValues.length - 2; i >= 0; i++) {
      double curr_sum = prev_sum + eigenValues[i];
      double curr_rel = eigenValues[i] / curr_sum * i;
      // not too weak?
      if(eigenValues[i] >= weakEigenvalue) {
        double contrast = curr_rel - prev_rel;
        if(contrast > maxContrast) {
          maxContrast = contrast;
          contrastMaximum = i + 1;
        }
      }
      prev_sum = curr_sum;
      prev_rel = curr_rel;
    }
    return contrastMaximum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * The noise tolerance level for weak eigenvectors
     */
    private double walpha;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(WeakEigenPairFilter.Par.EIGENPAIR_FILTER_WALPHA, DEFAULT_WALPHA) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> walpha = x);
    }

    @Override
    public DropEigenPairFilter make() {
      return new DropEigenPairFilter(walpha);
    }
  }
}
