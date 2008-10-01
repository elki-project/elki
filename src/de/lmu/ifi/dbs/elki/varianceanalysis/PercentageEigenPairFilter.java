package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * The PercentageEigenPairFilter sorts the eigenpairs in descending order of
 * their eigenvalues and marks the first eigenpairs, whose sum of eigenvalues is
 * higher than the given percentage of the sum of all eigenvalues as strong
 * eigenpairs.
 * 
 * @author Elke Achtert
 */

public class PercentageEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * OptionID for
   * {@link #ALPHA_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_ALPHA = OptionID.getOrCreateOptionID("pca.filter.alpha",
      "The share (0.0 to 1.0) of variance that needs to be explained by the 'strong' eigenvectors."
      + "The filter class will choose the number of strong eigenvectors by this share.");

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Parameter alpha.
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(EIGENPAIR_FILTER_ALPHA, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  private double alpha;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in descending order
   * of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public PercentageEigenPairFilter() {
    super();
    addOption(ALPHA_PARAM);
  }

  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if(this.debug) {
      msg.append("\nalpha = ").append(alpha);
      msg.append("\nsortedEigenPairs = ").append(eigenPairs);
    }

    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // determine sum of eigenvalues
    double totalSum = 0;
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      totalSum += eigenPair.getEigenvalue();
    }
    if(this.debug) {
      msg.append("\ntotalSum = ").append(totalSum);
    }

    // determine strong and weak eigenpairs
    double currSum = 0;
    boolean found = false;
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      currSum += eigenPair.getEigenvalue();
      if(currSum / totalSum >= alpha) {
        if(!found) {
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
    if(this.debug) {
      msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      debugFine(msg.toString());
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(PercentageEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " + "of their eigenvalues and returns the first eigenpairs, whose sum of" + "eigenvalues is higher than the given percentage of the sum of all eigenvalues.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  // todo comment
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // alpha
    alpha = ALPHA_PARAM.getValue();

    return remainingParameters;
  }
}
