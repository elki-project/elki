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
 * The WeakEigenPairFilter sorts the eigenpairs in descending order of their
 * eigenvalues and returns the first eigenpairs who are above the average mark
 * as "strong", the others as "weak".
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Weak Eigenpair Filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and returns those eigenpairs, whose eigenvalue is above the average ('expected') eigenvalue.")
public class WeakEigenPairFilter implements EigenPairFilter {
  /**
   * The default value for walpha.
   */
  public static final double DEFAULT_WALPHA = 0.95;

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double walpha;

  /**
   * Constructor.
   * 
   * @param walpha
   */
  public WeakEigenPairFilter(double walpha) {
    super();
    this.walpha = walpha;
  }

  @Override
  public int filter(double[] eigenValues) {
    // determine sum of eigenvalues
    double totalSum = 0;
    for(int i = 0; i < eigenValues.length; i++) {
      totalSum += eigenValues[i];
    }
    double expectEigenvalue = totalSum / eigenValues.length * walpha;

    // determine strong and weak eigenpairs
    for(int i = 0; i < eigenValues.length; i++) {
      if(eigenValues[i] <= expectEigenvalue) {
        return i;
      }
    }

    // the code using this method doesn't expect an empty strong set,
    // if we didn't find any strong ones, we make all vectors strong
    return eigenValues.length;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * OptionID for the weak alpha value of {@link WeakEigenPairFilter},
     * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.ProgressiveEigenPairFilter}
     * and
     * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.SignificantEigenPairFilter}
     */
    public static final OptionID EIGENPAIR_FILTER_WALPHA = new OptionID("pca.filter.weakalpha", "The minimum strength of the statistically expected variance (1/n) share an eigenvector " + "needs to have to be considered 'strong'.");

    /**
     * The threshold for strong eigenvectors: the strong eigenvectors explain a
     * portion of at least alpha of the total variance.
     */
    private double walpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter walphaP = new DoubleParameter(EIGENPAIR_FILTER_WALPHA, DEFAULT_WALPHA) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(walphaP)) {
        walpha = walphaP.getValue();
      }
    }

    @Override
    protected WeakEigenPairFilter makeInstance() {
      return new WeakEigenPairFilter(walpha);
    }
  }
}
