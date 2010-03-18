package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * The NormalizingEigenPairFilter normalizes all eigenvectors s.t. <eigenvector,
 * eigenvector> * eigenvalue = 1, where <,> is the standard dot product
 * 
 * @author Simon Paradies
 */
@Title("Perecentage based Eigenpair filter")
@Description("Normalizes all eigenpairs, consisting of eigenvalue e and eigenvector v such that <v,v> * e = 1, where <,> is the standard dot product.")
public class NormalizingEigenPairFilter extends AbstractLoggable implements EigenPairFilter, Parameterizable {
  /**
   * Provides a new EigenPairFilter that normalizes all eigenvectors s.t.
   * eigenvalue * <eigenvector, eigenvector> = 1, where <,> is the standard dot
   * product
   */
  public NormalizingEigenPairFilter() {
    super();
  }

  public FilteredEigenPairs filter(final SortedEigenPairs eigenPairs) {
    // initialize strong and weak eigenpairs
    // all normalized eigenpairs are regarded as strong
    final List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    final List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();
    for(int i = 0; i < eigenPairs.size(); i++) {
      final EigenPair eigenPair = eigenPairs.getEigenPair(i);
      normalizeEigenPair(eigenPair);
      strongEigenPairs.add(eigenPair);
    }
    if(logger.isDebugging()) {
      final StringBuffer msg = new StringBuffer();
      msg.append("strong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      logger.debugFine(msg.toString());
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
  }

  /**
   * Normalizes an eigenpair consisting of eigenvector v and eigenvalue e s.t.
   * <v,v> * e = 1
   * 
   * @param eigenPair the eigenpair to be normalized
   * 
   */
  private void normalizeEigenPair(final EigenPair eigenPair) {
    final Matrix eigenvector = eigenPair.getEigenvector();
    final double scaling = 1.0 / Math.sqrt(eigenPair.getEigenvalue()) * eigenvector.normF();
    eigenvector.scaleColumn(0, scaling);
  }
}