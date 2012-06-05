package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The "drop" filter looks for the largest drop in normalized relative
 * eigenvalues.
 * 
 * Let s_1 .. s_n be the eigenvalues.
 * 
 * Let a_k := 1/(n-k) sum_{i=k..n} s_i
 * 
 * Then r_k := s_k / a_k is the relative eigenvalue.
 * 
 * The drop filter searches for argmax_k r_k / r_{k+1}
 * 
 * @author Erich Schubert
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
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // default value is "all strong".
    int contrastMaximum = eigenPairs.size() - 1;
    double maxContrast = 1.0;

    double[] ev = eigenPairs.eigenValues();
    // calc the eigenvalue sum.
    double eigenValueSum = 0.0;
    for(int i = 0; i < ev.length; i++) {
      eigenValueSum += ev[i];
    }
    // Minimum value
    final double weakEigenvalue = walpha * eigenValueSum / ev.length;
    // Now find the maximum contrast, scanning backwards.
    double prev_sum = ev[ev.length - 1];
    double prev_rel = 0.0;
    for(int i = 2; i <= ev.length; i++) {
      double curr_sum = prev_sum + ev[ev.length - i];
      double curr_rel = ev[ev.length - i] / (curr_sum / i);
      // not too weak?
      if(ev[ev.length - i] >= weakEigenvalue) {
        double contrast = curr_rel - prev_rel;
        if(contrast > maxContrast) {
          maxContrast = contrast;
          contrastMaximum = ev.length - i;
        }
      }
      prev_sum = curr_sum;
      prev_rel = curr_rel;
    }

    for(int i = 0; i <= contrastMaximum; i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      strongEigenPairs.add(eigenPair);
    }
    for(int i = contrastMaximum + 1; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      weakEigenPairs.add(eigenPair);
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    private double walpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter walphaP = new DoubleParameter(WeakEigenPairFilter.EIGENPAIR_FILTER_WALPHA, new GreaterEqualConstraint(0.0), DEFAULT_WALPHA);
      if(config.grab(walphaP)) {
        walpha = walphaP.getValue();
      }
    }

    @Override
    protected DropEigenPairFilter makeInstance() {
      return new DropEigenPairFilter(walpha);
    }
  }
}