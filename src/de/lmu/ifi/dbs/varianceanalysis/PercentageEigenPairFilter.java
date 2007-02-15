package de.lmu.ifi.dbs.varianceanalysis;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * The PercentageEigenPairFilter sorts the eigenpairs in decending order
 * of their eigenvalues and marks the first eigenpairs, whose sum of
 * eigenvalues is higher than the given percentage of the sum of all eigenvalues
 * as strong eigenpairs.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class PercentageEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<double>a double between 0 and 1 specifying " +
                                       "the threshold for strong eigenvectors: " +
                                       "the strong eigenvectors explain a " +
                                       "portion of at least alpha of the total variance " +
                                       "(default is alpha = " + DEFAULT_ALPHA + ")";

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  private double alpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all eigenvalues
   * as string eigenpairs.
   */
  public PercentageEigenPairFilter() {
    super();

    ArrayList<ParameterConstraint<Number>> constraints = new ArrayList<ParameterConstraint<Number>>();
    constraints.add(new GreaterConstraint(0));
    constraints.add(new LessConstraint(1));
    DoubleParameter alpha = new DoubleParameter(ALPHA_P,ALPHA_D,constraints);
    alpha.setDefaultValue(DEFAULT_ALPHA);
    optionHandler.put(ALPHA_P, alpha);
  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs)
   */
  public FilteredEigenPairs  filter(SortedEigenPairs eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      msg.append("\nalpha = ").append(alpha);
      msg.append("\nsortedEigenPairs = ").append(eigenPairs);
    }

    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // determine sum of eigenvalues
    double totalSum = 0;
    for (int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      totalSum += eigenPair.getEigenvalue();
    }
    if (this.debug) {
      msg.append("\ntotalSum = ").append(totalSum);
    }

    // determine strong and weak eigenpairs
    double currSum = 0;
    boolean found = false;
    for (int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      currSum += eigenPair.getEigenvalue();
      if (currSum / totalSum >= alpha) {
        if (!found) {
          found = true;
          strongEigenPairs.add(eigenPair);
        }
        else {
          weakEigenPairs.add(eigenPair);
        }
      }
      else {
        strongEigenPairs.add(eigenPair);
      }
    }
    if (this.debug) {
      msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      debugFine(msg.toString());
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(PercentageEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " +
                       "of their eigenvalues and returns the first eigenpairs, whose sum of" +
                       "eigenvalues is higher than the given percentage of the sum of all eigenvalues.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //alpha
    alpha = (Double)optionHandler.getOptionValue(ALPHA_P);

    return remainingParameters;
  }
}
