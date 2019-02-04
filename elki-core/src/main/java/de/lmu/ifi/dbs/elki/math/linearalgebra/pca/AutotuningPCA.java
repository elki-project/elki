/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.*;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.SignificantEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Performs a self-tuning local PCA based on the covariance matrices of given
 * objects. At most the closest 'k' points are used in the calculation and a
 * weight function is applied.
 * <p>
 * The number of points used depends on when the strong eigenvectors exhibit the
 * clearest correlation.
 * <p>
 * Reference:
 * <p>
 * A General Framework for Increasing the Robustness of PCA-Based Correlation
 * Clustering Algorithms<br>
 * Hans-Peter Kriegel and Peer Kröger and Erich Schubert and Arthur Zimek<br>
 * Proc. 20th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM)
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "A General Framework for Increasing the Robustness of PCA-based Correlation Clustering Algorithms", //
    booktitle = "Proc. 20th Intl. Conf. on Scientific and Statistical Database Management (SSDBM)", //
    url = "https://doi.org/10.1007/978-3-540-69497-7_27", //
    bibkey = "DBLP:conf/ssdbm/KriegelKSZ08")
public class AutotuningPCA extends PCARunner {
  /**
   * Filter to select eigenvectors.
   */
  private EigenPairFilter filter;

  /**
   * Constructor.
   * 
   * @param covarianceMatrixBuilder Covariance matrix builder
   * @param filter Filter to select eigenvectors
   */
  public AutotuningPCA(CovarianceMatrixBuilder covarianceMatrixBuilder, EigenPairFilter filter) {
    super(covarianceMatrixBuilder);
    this.filter = filter;
  }

  @Override
  public PCAResult processIds(DBIDs ids, Relation<? extends NumberVector> database) {
    // Assume Euclidean distance. In the context of PCA, the neighborhood should
    // be L2-spherical to be unbiased.
    Centroid center = Centroid.make(database, ids);
    ModifiableDoubleDBIDList dres = DBIDUtil.newDistanceDBIDList(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double dist = EuclideanDistanceFunction.STATIC.distance(center, database.get(iter));
      dres.add(dist, iter);
    }
    dres.sort();
    return processQueryResult(dres, database);
  }

  /**
   * Candidate
   * 
   * @author Erich Schubert
   */
  private static class Cand {
    /** Candidate matrix */
    double[][] m;

    /** Score */
    double explain;

    /** Dimensionality */
    int dim;

    /**
     * Constructor.
     * 
     * @param m Matrix
     * @param explain Explains core
     * @param dim Dimensionality
     */
    Cand(double[][] m, double explain, int dim) {
      this.m = m;
      this.explain = explain;
      this.dim = dim;
    }
  }

  @Override
  public PCAResult processQueryResult(DoubleDBIDList results, Relation<? extends NumberVector> database) {
    assertSortedByDistance(results);
    final int dim = RelationUtil.dimensionality(database);

    double[][][] best = new double[dim][][];
    double[] beststrength = new double[dim];
    Arrays.fill(beststrength, -1);
    int[] bestk = new int[dim];
    // 'history'
    LinkedList<Cand> prev = new LinkedList<>();
    // TODO: starting parameter shouldn't be hardcoded...
    int smooth = 3;
    int startk = Math.min(4, results.size() - 1);
    // TODO: add smoothing options, handle border cases better.
    for(int k = startk; k < results.size(); k++) {
      double[][] covMat = covarianceMatrixBuilder.processQueryResults(results, database);
      EigenvalueDecomposition evd = new EigenvalueDecomposition(covMat);
      double[] evs = reversed(evd.getRealEigenvalues().clone());

      // correlationDimension = #strong EV
      int thisdim = filter.filter(evs);

      // FIXME: handle the case of no strong EVs.
      assert ((thisdim > 0) && (thisdim <= dim));
      double thisexplain = computeExplainedVariance(evs, thisdim);

      prev.add(new Cand(covMat, thisexplain, thisdim));

      if(prev.size() >= 2 * smooth + 1) {
        // all the same dimension?
        boolean samedim = true;
        for(Iterator<Cand> it = prev.iterator(); it.hasNext();) {
          if(it.next().dim != thisdim) {
            samedim = false;
          }
        }
        if(samedim) {
          // average their explain values
          double avgexplain = 0.0;
          for(Iterator<Cand> it = prev.iterator(); it.hasNext();) {
            avgexplain += it.next().explain;
          }
          avgexplain /= prev.size();

          if(avgexplain > beststrength[thisdim - 1]) {
            beststrength[thisdim - 1] = avgexplain;
            best[thisdim - 1] = prev.get(smooth).m;
            bestk[thisdim - 1] = k - smooth;
          }
        }
        prev.removeFirst();
      }
    }
    // Try all dimensions, lowest first.
    for(int i = 0; i < dim; i++) {
      if(beststrength[i] > 0.) {
        // If the best was the lowest or the biggest k, skip it!
        if(bestk[i] == startk + smooth || bestk[i] == results.size() - smooth - 1) {
          continue;
        }
        // We stop at the lowest dimension that did the job for us.
        return processCovarMatrix(best[i]);
      }
    }
    // NOTE: if we didn't get a 'maximum' anywhere, we end up with the data from
    // the last run of the loop above. I.e. PCA on the full data set. That is
    // intended.
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Sort an array of doubles in descending order.
   *
   * @param a Values
   * @return Values in descending order
   */
  private static double[] reversed(double[] a) {
    // TODO: there doesn't appear to be a nicer version in Java, unfortunately.
    Arrays.sort(a);
    for(int i = 0, j = a.length - 1; i < j; i++, j--) {
      double tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }
    return a;
  }

  /**
   * Compute the explained variance for a filtered EigenPairs.
   * 
   * @param eigenValues Eigen values
   * @param filteredEigenPairs Filtered eigenpairs
   * @return explained variance by the strong eigenvectors.
   */
  private double computeExplainedVariance(double[] eigenValues, int filteredEigenPairs) {
    double strongsum = 0., weaksum = 0.;
    for(int i = 0; i < filteredEigenPairs; i++) {
      strongsum += eigenValues[i];
    }
    for(int i = filteredEigenPairs; i < eigenValues.length; i++) {
      weaksum += eigenValues[i];
    }
    return strongsum / (strongsum + weaksum);
  }

  /**
   * Ensure that the results are sorted by distance.
   * 
   * @param results Results to process
   */
  private void assertSortedByDistance(DoubleDBIDList results) {
    // TODO: sort results instead?
    double dist = -1.0;
    boolean sorted = true;
    for(DoubleDBIDListIter it = results.iter(); it.valid(); it.advance()) {
      double qr = it.doubleValue();
      if(qr < dist) {
        sorted = false;
      }
      dist = qr;
    }
    if(!sorted) {
      try {
        ModifiableDoubleDBIDList.class.cast(results).sort();
      }
      catch(ClassCastException | UnsupportedOperationException e) {
        LoggingUtil.warning("WARNING: results not sorted by distance!", e);
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends PCARunner.Parameterizer {
    /**
     * Parameter for filtering eigenvectors.
     */
    public static final OptionID PCA_EIGENPAIR_FILTER = new OptionID("autopca.filter", "Filter for selecting eigenvectors during autotuning PCA.");

    /**
     * Filter to select eigenvectors.
     */
    private EigenPairFilter filter;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<>(PCA_EIGENPAIR_FILTER, EigenPairFilter.class, SignificantEigenPairFilter.class);
      if(config.grab(filterP)) {
        filter = filterP.instantiateClass(config);
      }
    }

    @Override
    protected AutotuningPCA makeInstance() {
      return new AutotuningPCA(covarianceMatrixBuilder, filter);
    }
  }
}
