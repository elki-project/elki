package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.util.List;

/**
 * The eigenpair filter is used to filter eigenpairs (i.e. eigenvectors
 * and their corresponding eigenvalues) which are a result of a
 * Variance Analysis Algorithm, e.g. Principal Component Analysis.
 * The eigenpairs are filtered into two types: strong and weak eigenpairs,
 * where strong eigenpairs having high variances
 * and weak eigenpairs having small variances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface EigenPairFilter extends Parameterizable {

  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs,
   * where strong eigenpairs having high variances
   * and weak eigenpairs having small variances.
   *
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   *                   their corresponding eigenvalues)
   */
  public void filter(EigenPair[] eigenPairs);

  /**
   * Returns the strong eigenPairs having high variances.
   *
   * @return the strong eigenPairs
   */
  public List<EigenPair> getStrongEigenPairs();

  /**
   * Returns the weak eigenPairs having small variances.
   *
   * @return the weak eigenPairs
   */
  public List<EigenPair> getWeakEigenPairs();
}
