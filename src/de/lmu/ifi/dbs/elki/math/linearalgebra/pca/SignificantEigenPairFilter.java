package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
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
 */
public class SignificantEigenPairFilter extends AbstractLoggable implements EigenPairFilter, Parameterizable {
  /**
   * The default value for walpha.
   * Not used by default, we're going for maximum contrast only.
   */
  public static final double DEFAULT_WALPHA = 0.0;

  /**
   * Parameter weak alpha.
   */
  private final DoubleParameter WALPHA_PARAM = new DoubleParameter(WeakEigenPairFilter.EIGENPAIR_FILTER_WALPHA,
      new GreaterEqualConstraint(0.0), DEFAULT_WALPHA);

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double walpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public SignificantEigenPairFilter(Parameterization config) {
    super();
    if (config.grab(WALPHA_PARAM)) {
      walpha = WALPHA_PARAM.getValue();      
    }
  }

  /**
   * Filter eigenpairs
   */
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // default value is "all strong".
    int contrastMaximum = eigenPairs.size() - 1;
    double maxContrast = 0.0;
    // calc the eigenvalue sum.
    double eigenValueSum = 0.0;
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      eigenValueSum += eigenPair.getEigenvalue();
    }
    double weakEigenvalue = eigenValueSum / eigenPairs.size() * walpha;
    // now find the maximum contrast.
    double currSum = eigenPairs.getEigenPair(eigenPairs.size() - 1).getEigenvalue();
    for(int i = eigenPairs.size() - 2; i >= 0; i--) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      currSum += eigenPair.getEigenvalue();
      // weak?
      if(eigenPair.getEigenvalue() < weakEigenvalue)
        continue;
      double contrast = eigenPair.getEigenvalue() / (currSum / (eigenPairs.size() - i));
      if(contrast > maxContrast) {
        maxContrast = contrast;
        contrastMaximum = i;
      }
    }

    for(int i = 0; i <= contrastMaximum /* && i < eigenPairs.size() */; i++) {
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
   * Get parameter description
   */
  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(SignificantEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order of their eigenvalues and looks for the maxmimum contrast of " + "current Eigenvalue / average of remaining Eigenvalues.\n");
    return description.toString();
  }
}