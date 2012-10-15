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
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The RelativeEigenPairFilter sorts the eigenpairs in descending order of their
 * eigenvalues and marks the first eigenpairs who are a certain factor above the
 * average of the remaining eigenvalues.
 * 
 * It is closely related to the WeakEigenPairFilter, and differs mostly by
 * comparing to the remaining Eigenvalues, not to the total sum.
 * 
 * There are some situations where one or the other is superior, especially when
 * it comes to handling nested clusters and strong global correlations that are
 * not too interesting. These benefits usually only make a difference at higher
 * dimensionalities.
 * 
 * @author Erich Schubert
 */
@Title("Relative EigenPair Filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and returns those eigenpairs, whose eigenvalue is " + "above the average ('expected') eigenvalue of the remaining eigenvectors.")
public class RelativeEigenPairFilter implements EigenPairFilter {
  /**
   * Parameter relative alpha.
   */
  public static final OptionID EIGENPAIR_FILTER_RALPHA = OptionID.getOrCreateOptionID("pca.filter.relativealpha", "The sensitivity niveau for weak eigenvectors: An eigenvector which is at less than " + "the given share of the statistical average variance is considered weak.");

  /**
   * The default value for ralpha.
   */
  public static final double DEFAULT_RALPHA = 1.1;

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double ralpha;

  /**
   * Constructor.
   * 
   * @param ralpha
   */
  public RelativeEigenPairFilter(double ralpha) {
    super();
    this.ralpha = ralpha;
  }

  /**
   * Filter eigenpairs
   */
  @Override
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // default value is "all strong".
    int contrastAtMax = eigenPairs.size() - 1;
    // find the last eigenvector that is considered 'strong' by the weak rule
    // applied to the remaining vectors only
    double eigenValueSum = eigenPairs.getEigenPair(eigenPairs.size() - 1).getEigenvalue();
    for (int i = eigenPairs.size() - 2; i >= 0; i--) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      eigenValueSum += eigenPair.getEigenvalue();
      double needEigenvalue = eigenValueSum / (eigenPairs.size() - i) * ralpha;
      if (eigenPair.getEigenvalue() >= needEigenvalue) {
        contrastAtMax = i;
        break;
      }
    }

    for (int i = 0; i <= contrastAtMax /* && i < eigenPairs.size() */; i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      strongEigenPairs.add(eigenPair);
    }
    for (int i = contrastAtMax + 1; i < eigenPairs.size(); i++) {
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
    protected double ralpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter ralphaP = new DoubleParameter(EIGENPAIR_FILTER_RALPHA, DEFAULT_RALPHA);
      ralphaP.addConstraint(new GreaterEqualConstraint(0.0));
      if (config.grab(ralphaP)) {
        ralpha = ralphaP.getValue();
      }
    }

    @Override
    protected RelativeEigenPairFilter makeInstance() {
      return new RelativeEigenPairFilter(ralpha);
    }
  }
}
