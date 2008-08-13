package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * The WeakEigenPairFilter sorts the eigenpairs in decending order of their
 * eigenvalues and returns the first eigenpairs who are above the average mark
 * as "strong", the others as "weak".
 * 
 * @author Erich Schubert
 */

public class WeakEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * The default value for walpha.
   */
  public static final double DEFAULT_WALPHA = 0.95;

  /**
   * Option string for parameter walpha.
   */
  public static final String WALPHA_P = "walpha";

  /**
   * Description for parameter walpha.
   */
  public static final String WALPHA_D = "<double>a double larger than 0 specifying " + "the sensitivity niveau for weak eigenvectors: " + "An eigenvector which is less than walpha times " + "the statistical average variance is considered weak. " + "(default is walpha = " + DEFAULT_WALPHA + ")";

  /**
   * The noise tolerance niveau for weak eigenvectors
   */
  private double walpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public WeakEigenPairFilter() {
    super();

    ArrayList<ParameterConstraint<Number>> constraints = new ArrayList<ParameterConstraint<Number>>();
    constraints.add(new GreaterEqualConstraint(0));
    DoubleParameter walpha = new DoubleParameter(WALPHA_P, WALPHA_D, constraints);
    walpha.setDefaultValue(DEFAULT_WALPHA);
    optionHandler.put(walpha);
  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs)
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

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(WeakEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " + "of their eigenvalues and returns those eigenpairs, whose eigenvalue is " + "above the average ('expected') eigenvalue.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // alpha
    walpha = (Double) optionHandler.getOptionValue(WALPHA_P);

    return remainingParameters;
  }
}