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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter;

import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The SignificantEigenPairFilter sorts the eigenpairs in descending order of
 * their eigenvalues and chooses the contrast of an Eigenvalue to the remaining
 * Eigenvalues is maximal.
 *
 * It is closely related to the WeakEigenPairFilter and RelativeEigenPairFilter.
 * But while the RelativeEigenPairFilter chooses the highest dimensionality that
 * satisfies the relative alpha levels, the SignificantEigenPairFilter will
 * chose the local dimensionality such that the 'contrast' is maximal.
 *
 * There are some situations where one or the other is superior, especially when
 * it comes to handling nested clusters and strong global correlations that are
 * not too interesting. These benefits usually only make a difference at higher
 * dimensionalities.
 *
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Significant EigenPair Filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and looks for the maxmimum contrast of current Eigenvalue / average of remaining Eigenvalues.")
public class SignificantEigenPairFilter implements EigenPairFilter {
  /**
   * The default value for walpha. Not used by default, we're going for maximum
   * contrast only.
   */
  public static final double DEFAULT_WALPHA = 0.0;

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double walpha;

  /**
   * Constructor.
   *
   * @param walpha
   */
  public SignificantEigenPairFilter(double walpha) {
    super();
    this.walpha = walpha;
  }

  @Override
  public int filter(double[] eigenPairs) {
    // default value is "all strong".
    int contrastMaximum = eigenPairs.length;
    double maxContrast = 0.0;
    // calc the eigenvalue sum.
    double eigenValueSum = 0.0;
    for(int i = 0; i < eigenPairs.length; i++) {
      eigenValueSum += eigenPairs[i];
    }
    double weakEigenvalue = eigenValueSum / eigenPairs.length * walpha;
    // now find the maximum contrast.
    double currSum = eigenPairs[eigenPairs.length - 1];
    for(int i = eigenPairs.length - 2; i >= 0; i--) {
      currSum += eigenPairs[i];
      // weak?
      if(eigenPairs[i] < weakEigenvalue) {
        break;
      }
      double contrast = eigenPairs[i] / (currSum / (eigenPairs.length - i));
      if(contrast > maxContrast) {
        maxContrast = contrast;
        contrastMaximum = i + 1;
      }
    }
    return contrastMaximum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    private double walpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter walphaP = new DoubleParameter(WeakEigenPairFilter.Parameterizer.EIGENPAIR_FILTER_WALPHA, DEFAULT_WALPHA) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(walphaP)) {
        walpha = walphaP.getValue();
      }
    }

    @Override
    protected SignificantEigenPairFilter makeInstance() {
      return new SignificantEigenPairFilter(walpha);
    }
  }
}
