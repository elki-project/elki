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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The PercentageEigenPairFilter sorts the eigenpairs in descending order of
 * their eigenvalues and marks the first eigenpairs, whose sum of eigenvalues is
 * higher than the given percentage of the sum of all eigenvalues as strong
 * eigenpairs.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
@Title("Percentage based Eigenpair filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and returns the first eigenpairs, whose sum of eigenvalues is higher than the given percentage of the sum of all eigenvalues.")
public class PercentageEigenPairFilter implements EigenPairFilter {
  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  private double alpha;

  /**
   * Constructor.
   * 
   * @param alpha
   */
  public PercentageEigenPairFilter(double alpha) {
    super();
    this.alpha = alpha;
  }

  @Override
  public int filter(double[] eigenValues) {
    // determine sum of eigenvalues
    double totalSum = 0;
    for(int i = 0; i < eigenValues.length; i++) {
      totalSum += eigenValues[i];
    }

    // determine strong and weak eigenpairs
    double currSum = 0;
    for(int i = 0; i < eigenValues.length; i++) {
      currSum += eigenValues[i];
      if(currSum / totalSum >= alpha) {
        return i + 1;
      }
    }
    return eigenValues.length;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The threshold for 'strong' eigenvectors: the 'strong' eigenvectors
     * explain a portion of at least alpha of the total variance.
     */
    public static final OptionID ALPHA_ID = new OptionID("pca.filter.alpha", "The share (0.0 to 1.0) of variance that needs to be explained by the 'strong' eigenvectors. The filter class will choose the number of strong eigenvectors by this share.");

    /**
     * The threshold for strong eigenvectors: the strong eigenvectors explain a
     * portion of at least alpha of the total variance.
     */
    private double alpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, DEFAULT_ALPHA) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected PercentageEigenPairFilter makeInstance() {
      return new PercentageEigenPairFilter(alpha);
    }
  }
}
