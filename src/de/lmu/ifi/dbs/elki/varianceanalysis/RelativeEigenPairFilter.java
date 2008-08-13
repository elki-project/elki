package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * The RelativeEigenPairFilter sorts the eigenpairs in decending order of their
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

public class RelativeEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * The default value for ralpha.
   */
  public static final double DEFAULT_RALPHA = 1.1;

  /**
   * Option string for parameter ralpha.
   */
  public static final String RALPHA_P = "ralpha";

  /**
   * Description for parameter ralpha.
   */
  public static final String RALPHA_D = "<double>a double larger than 1 specifying " + "the sensitivity niveau for weak eigenvectors: " + "An eigenvector which is at less than ralpha times " + "the statistical average variance is considered weak. " + "(default is ralpha = " + DEFAULT_RALPHA + ")";

  /**
   * The noise tolerance niveau for weak eigenvectors
   */
  private double ralpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public RelativeEigenPairFilter() {
    super();

    ArrayList<ParameterConstraint<Number>> constraints = new ArrayList<ParameterConstraint<Number>>();
    constraints.add(new GreaterEqualConstraint(1));
    DoubleParameter ralpha = new DoubleParameter(RALPHA_P, RALPHA_D, constraints);
    ralpha.setDefaultValue(DEFAULT_RALPHA);
    optionHandler.put(ralpha);

  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs)
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
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RelativeEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " + "of their eigenvalues and returns those eigenpairs, whose eigenvalue is " + "above the average ('expected') eigenvalue of the remaining eigenvectors.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // alpha
    ralpha = (Double) optionHandler.getOptionValue(RALPHA_P);

    return remainingParameters;
  }
}