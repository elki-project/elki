package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * The PercentageEigenPairFilter sorts the eigenpairs in descending order of
 * their eigenvalues and marks the first eigenpairs, whose sum of eigenvalues is
 * higher than the given percentage of the sum of all eigenvalues as strong
 * eigenpairs.
 * 
 * @author Elke Achtert
 */
@Title("Percentage based Eigenpair filter")
@Description("Sorts the eigenpairs in decending order of their eigenvalues and returns the first eigenpairs, whose sum of eigenvalues is higher than the given percentage of the sum of all eigenvalues.")
public class PercentageEigenPairFilter extends AbstractLoggable implements EigenPairFilter, Parameterizable {
  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_ALPHA = OptionID.getOrCreateOptionID("pca.filter.alpha", "The share (0.0 to 1.0) of variance that needs to be explained by the 'strong' eigenvectors." + "The filter class will choose the number of strong eigenvectors by this share.");

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
   * Provides a new EigenPairFilter that sorts the eigenpairs in descending
   * order of their eigenvalues and marks the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all
   * eigenvalues as string eigenpairs.
   */
  public PercentageEigenPairFilter(Parameterization config) {
    super();
    if(config.grab(ALPHA_PARAM)) {
      alpha = ALPHA_PARAM.getValue();
    }
  }

  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if(logger.isDebugging()) {
      msg.append("alpha = ").append(alpha);
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
    if(logger.isDebugging()) {
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
    if(logger.isDebugging()) {
      msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      logger.debugFine(msg.toString());
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }
}
