package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.util.List;

/**
 * The eigenpair filter is used to exclude some unfit data after
 * it has been analysed using a Variance Analysis Algorithm,
 * e.g. Principal Component Analysis.
 * It is mainly used to exlude data, whose variance is too small.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface EigenPairFilter extends Parameterizable {

  /**
   * This function is called after the data has
   * been analysed using a Variance Analysis Algorithm.
   *
   * @param eigenPairs  the computed eigenPairs (i.e. the computed
   * eigenvectors and their corresponding eigenvalues)
   */
  public void passEigenPairs(EigenPair[] eigenPairs);

  /**
   * Returns the strong eigenPairs, which
   * were not excluded.
   *
   * @return the strong eigenPairs
   */
  public List<EigenPair> getStrongEigenPairs();

   /**
   * Returns the weak eigenPairs, which
   * have been excluded.
   *
   * @return the weak eigenPairs
   */
  public List<EigenPair> getWeakEigenPairs();
}
