package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * The FirstNEigenPairFilter marks the n highest eigenpairs
 * as strong eigenpairs, where n is a user specified number.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class FirstNEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {

  /**
   * Option string for parameter n.
   */
  public static final String N_P = "n";

  /**
   * Description for parameter alpha.
   */
  public static final String N_D = "an integer specifying " +
                                   "the number of strong eigenvectors: " +
                                   "n eigenvectors with the n highest eigenvalues " +
                                   "are marked as strong eigenvectors.";

  /**
   * The threshold for strong eigenvectors: n eigenvectors with
   * the n highest eigenvalues are marked as strong eigenvectors.
   */
  private double n;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and marks the first n eigenpairs as strong eigenpairs.
   */
  public FirstNEigenPairFilter() {
    super();
//    this.debug = true;

    optionHandler.put(N_P, new IntParameter(N_P, N_D, new GreaterEqualConstraint(0)));
  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs)
   */
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      msg.append("\nsortedEigenPairs " + eigenPairs.toString());
      msg.append("\nn = ").append(n);
    }

    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // determine strong and weak eigenpairs
    for (int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      if (i < n) {
        strongEigenPairs.add(eigenPair);
      }
      else {
        weakEigenPairs.add(eigenPair);
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
    description.append(FirstNEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " +
                       "of their eigenvalues and marks the first n eigenpairs " +
                       "as strong eigenpairs.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //n
    n = (Integer)optionHandler.getOptionValue(FirstNEigenPairFilter.N_P);
    
    return remainingParameters;
  }
}
