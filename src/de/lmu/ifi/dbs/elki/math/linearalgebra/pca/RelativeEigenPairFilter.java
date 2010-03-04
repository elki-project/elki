package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
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
public class RelativeEigenPairFilter extends AbstractLoggable implements EigenPairFilter, Parameterizable {
  /**
   * OptionID for {@link #RALPHA_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_RALPHA = OptionID.getOrCreateOptionID("pca.filter.relativealpha", "The sensitivity niveau for weak eigenvectors: An eigenvector which is at less than " + "the given share of the statistical average variance is considered weak.");

  /**
   * The default value for ralpha.
   */
  public static final double DEFAULT_RALPHA = 1.1;

  /**
   * Parameter relative alpha.
   */
  private final DoubleParameter RALPHA_PARAM = new DoubleParameter(EIGENPAIR_FILTER_RALPHA, new GreaterEqualConstraint(0.0), DEFAULT_RALPHA);

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double ralpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in descending
   * order of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public RelativeEigenPairFilter(Parameterization config) {
    super();
    if (config.grab(RALPHA_PARAM)) {
      ralpha = RALPHA_PARAM.getValue();
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
    int contrastAtMax = eigenPairs.size() - 1;
    // find the last eigenvector that is considered 'strong' by the weak rule
    // applied to the remaining vectors only
    double eigenValueSum = eigenPairs.getEigenPair(eigenPairs.size() - 1).getEigenvalue();
    for(int i = eigenPairs.size() - 2; i >= 0; i--) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      eigenValueSum += eigenPair.getEigenvalue();
      double needEigenvalue = eigenValueSum / (eigenPairs.size() - i) * ralpha;
      if(eigenPair.getEigenvalue() >= needEigenvalue) {
        contrastAtMax = i;
        break;
      }
    }

    for(int i = 0; i <= contrastAtMax /* && i < eigenPairs.size() */; i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      strongEigenPairs.add(eigenPair);
    }
    for(int i = contrastAtMax + 1; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      weakEigenPairs.add(eigenPair);
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }

  /**
   * Get parameter description.
   */
  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(RelativeEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order of their eigenvalues and returns those eigenpairs, whose eigenvalue is " + "above the average ('expected') eigenvalue of the remaining eigenvectors.\n");
    return description.toString();
  }
}