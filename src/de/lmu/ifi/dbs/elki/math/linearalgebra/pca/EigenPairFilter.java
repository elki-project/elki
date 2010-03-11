package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;

/**
 * The eigenpair filter is used to filter eigenpairs (i.e. eigenvectors
 * and their corresponding eigenvalues) which are a result of a
 * Variance Analysis Algorithm, e.g. Principal Component Analysis.
 * The eigenpairs are filtered into two types: strong and weak eigenpairs,
 * where strong eigenpairs having high variances
 * and weak eigenpairs having small variances.
 *
 * @author Elke Achtert
 */

public interface EigenPairFilter extends Parameterizable {
  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs,
   * where strong eigenpairs having high variances
   * and weak eigenpairs having small variances.
   *
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   * @return the filtered eigenpairs
   */
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs);
}
