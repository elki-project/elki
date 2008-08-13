package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * The ProgressiveEigenPairFilter sorts the eigenpairs in decending order of
 * their eigenvalues and marks the first eigenpairs, whose sum of eigenvalues is
 * higher than the given percentage of the sum of all eigenvalues as strong
 * eigenpairs. In contrast to the PercentageEigenPairFilter, it will use a
 * percentage which changes linearly with the subspace dimensionality. This
 * makes the parameter more consistent for different dimensionalities and often
 * gives better results when clusters of different dimensionality exist, since
 * different percentage alpha levels might be appropriate for different
 * dimensionalities.
 * 
 * @author Erich Schubert
 * 
 * Example calculations of alpha levels:
 * 
 * In a 3D space, a progressive alpha value of 0.5 equals:
 * 
 * - 1D subspace: 50 % + 1/3 of remainder = 0.667
 * 
 * - 2D subspace: 50 % + 2/3 of remainder = 0.833
 * 
 * In a 4D space, a progressive alpha value of 0.5 equals:
 * 
 * - 1D subspace: 50% + 1/4 of remainder = 0.625
 * 
 * - 2D subspace: 50% + 2/4 of remainder = 0.750
 * 
 * - 3D subspace: 50% + 3/4 of remainder = 0.875
 * 
 * Reasoning why this improves over PercentageEigenPairFilter:
 * 
 * In a 100 dimensional space, a single Eigenvector representing over 85% of the
 * total variance is highly significant, whereas the strongest 85 Eigenvectors
 * together will by definition always represent at least 85% of the variance.
 * PercentageEigenPairFilter can thus not be used with these parameters and
 * detect both dimensionalities correctly.
 * 
 * The second parameter introduced here, walpha, serves a different function: It
 * prevents the eigenpair filter to use a statistically weak Eigenvalue just to
 * reach the intended level, e.g. 84% + 1% >= 85% when 1% is statistically very
 * weak.
 */

public class ProgressiveEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_PALPHA = 0.5;

  /**
   * Option string for parameter alpha.
   */
  public static final String PALPHA_P = "palpha";

  /**
   * Description for parameter alpha.
   * 
   * TODO: better description?
   */
  public static final String PALPHA_D = "<double>a double between 0 and 1 specifying " + "the threshold for strong eigenvectors: " + "the strong eigenvectors explain a " + "portion of at least alpha of the total variance " + "(default is alpha = " + DEFAULT_PALPHA + ")";

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  private double palpha;

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_WALPHA = 0.95;

  /**
   * Option string for parameter alpha.
   */
  public static final String WALPHA_P = "walpha";

  /**
   * Description for parameter alpha.
   */
  public static final String WALPHA_D = "<double>a double larger than 0 specifying " + "the sensitivity niveau for weak eigenvectors: " + "An eigenvector which is at less than walpha times " + "the statistical average variance is considered weak. " + "(default is walpha = " + DEFAULT_WALPHA + ")";

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
  public ProgressiveEigenPairFilter() {
    super();

    ArrayList<ParameterConstraint<Number>> constraints = new ArrayList<ParameterConstraint<Number>>();
    constraints.add(new GreaterConstraint(0));
    constraints.add(new LessConstraint(1));
    DoubleParameter palpha = new DoubleParameter(PALPHA_P, PALPHA_D, constraints);
    palpha.setDefaultValue(DEFAULT_PALPHA);
    optionHandler.put(palpha);

    ArrayList<ParameterConstraint<Number>> wconstraints = new ArrayList<ParameterConstraint<Number>>();
    wconstraints.add(new GreaterConstraint(0));
    DoubleParameter walpha = new DoubleParameter(WALPHA_P, WALPHA_D, wconstraints);
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
    double expectedVariance = totalSum / eigenPairs.size() * walpha;

    // determine strong and weak eigenpairs
    double currSum = 0;
    boolean found = false;
    int i;
    for(i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      // weak Eigenvector?
      if(eigenPair.getEigenvalue() < expectedVariance)
        break;
      currSum += eigenPair.getEigenvalue();
      // calculate progressive alpha level
      double alpha = 1.0 - (1.0 - palpha) * (1.0 - (i + 1) / eigenPairs.size());
      if(currSum / totalSum >= alpha || i == eigenPairs.size() - 1) {
        found = true;
        strongEigenPairs.add(eigenPair);
        break;
      }
    }
    // if we didn't hit our alpha level, we consider all vectors to be weak!
    if(!found) {
      assert (weakEigenPairs.size() == 0);
      weakEigenPairs = strongEigenPairs;
      strongEigenPairs = new ArrayList<EigenPair>();
    }
    for(; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      weakEigenPairs.add(eigenPair);
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
    description.append(ProgressiveEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " + "of their eigenvalues and returns the first eigenpairs, whose sum of " + "eigenvalues explains more than the given percentage of the unexpected variance.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // palpha
    palpha = (Double) optionHandler.getOptionValue(PALPHA_P);

    // walpha
    walpha = (Double) optionHandler.getOptionValue(WALPHA_P);

    return remainingParameters;
  }
}