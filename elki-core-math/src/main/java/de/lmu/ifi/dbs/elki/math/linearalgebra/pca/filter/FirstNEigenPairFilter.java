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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The FirstNEigenPairFilter marks the n highest eigenpairs as strong
 * eigenpairs, where n is a user specified number.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
@Title("First n Eigenpair filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and marks the first n eigenpairs as strong eigenpairs.")
public class FirstNEigenPairFilter implements EigenPairFilter {
  /**
   * The threshold for strong eigenvectors: n eigenvectors with the n highest
   * eigenvalues are marked as strong eigenvectors.
   */
  private int n;

  /**
   * Constructor.
   * 
   * @param n
   */
  public FirstNEigenPairFilter(int n) {
    super();
    this.n = n;
  }

  @Override
  public int filter(double[] eigenValues) {
    return n < eigenValues.length ? n : eigenValues.length;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter n
     */
    public static final OptionID EIGENPAIR_FILTER_N = new OptionID("pca.filter.n", "The number of strong eigenvectors: n eigenvectors with the n highest eigenvalues are marked as strong eigenvectors.");

    /**
     * The number of eigenpairs to keep.
     */
    protected int n = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter nP = new IntParameter(EIGENPAIR_FILTER_N) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(nP)) {
        n = nP.intValue();
      }
    }

    @Override
    protected FirstNEigenPairFilter makeInstance() {
      return new FirstNEigenPairFilter(n);
    }
  }
}
