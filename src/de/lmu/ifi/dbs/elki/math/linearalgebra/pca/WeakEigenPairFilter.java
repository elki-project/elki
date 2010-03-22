package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The WeakEigenPairFilter sorts the eigenpairs in descending order of their
 * eigenvalues and returns the first eigenpairs who are above the average mark
 * as "strong", the others as "weak".
 * 
 * @author Erich Schubert
 */
@Title("Weak Eigenpair Filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and returns those eigenpairs, whose eigenvalue is above the average ('expected') eigenvalue.")
public class WeakEigenPairFilter extends AbstractLoggable implements EigenPairFilter, Parameterizable {
  /**
   * OptionID for {@link #WALPHA_PARAM} and
   * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.ProgressiveEigenPairFilter#WALPHA_PARAM}
   * and
   * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.SignificantEigenPairFilter#WALPHA_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_WALPHA = OptionID.getOrCreateOptionID("pca.filter.weakalpha", "The minimum strength of the statistically expected variance (1/n) share an eigenvector " + "needs to have to be considered 'strong'.");

  /**
   * The default value for walpha.
   */
  public static final double DEFAULT_WALPHA = 0.95;

  /**
   * Parameter weak alpha.
   */
  private final DoubleParameter WALPHA_PARAM = new DoubleParameter(EIGENPAIR_FILTER_WALPHA, new GreaterEqualConstraint(0.0), DEFAULT_WALPHA);

  /**
   * The noise tolerance level for weak eigenvectors
   */
  private double walpha;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public WeakEigenPairFilter(Parameterization config) {
    super();
    if(config.grab(WALPHA_PARAM)) {
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

    // determine sum of eigenvalues
    double totalSum = 0;
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      totalSum += eigenPair.getEigenvalue();
    }
    double expectEigenvalue = totalSum / eigenPairs.size() * walpha;

    // determine strong and weak eigenpairs
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      if(eigenPair.getEigenvalue() > expectEigenvalue) {
        strongEigenPairs.add(eigenPair);
      }
      else {
        weakEigenPairs.add(eigenPair);
      }
    }

    // the code using this method doesn't expect an empty strong set,
    // if we didn't find any strong ones, we make all vectors strong
    if(strongEigenPairs.size() == 0) {
      return new FilteredEigenPairs(new ArrayList<EigenPair>(), weakEigenPairs);
    }
    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }
}