package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
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
 *
 * The number of points used depends on when the strong eigenvectors exhibit the
 * clearest correlation.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
title = "A General Framework for Increasing the Robustness of PCA-based Correlation Clustering Algorithms", //
booktitle = "Proceedings of the 20th International Conference on Scientific and Statistical Database Management (SSDBM), Hong Kong, China, 2008", //
url = "http://dx.doi.org/10.1007/978-3-540-69497-7_27")
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
   * 
   * @apiviz.exclude
   */
  private static class Cand {
    /** Candidate matrix */
    Matrix m;

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
    Cand(Matrix m, double explain, int dim) {
      this.m = m;
      this.explain = explain;
      this.dim = dim;
    }
  }

  @Override
  public PCAResult processQueryResult(DoubleDBIDList results, Relation<? extends NumberVector> database) {
    assertSortedByDistance(results);
    final int dim = RelationUtil.dimensionality(database);

    List<Matrix> best = new LinkedList<>();
    for(int i = 0; i < dim; i++) {
      best.add(null);
    }
    double[] beststrength = new double[dim];
    for(int i = 0; i < dim; i++) {
      beststrength[i] = -1;
    }
    int[] bestk = new int[dim];
    // 'history'
    LinkedList<Cand> prev = new LinkedList<>();
    // TODO: starting parameter shouldn't be hardcoded...
    int smooth = 3;
    int startk = 4;
    if(startk > results.size() - 1) {
      startk = results.size() - 1;
    }
    // TODO: add smoothing options, handle border cases better.
    for(int k = startk; k < results.size(); k++) {
      // sorted eigenpairs, eigenvectors, eigenvalues
      Matrix covMat = covarianceMatrixBuilder.processQueryResults(results, database);
      EigenvalueDecomposition evd = new EigenvalueDecomposition(covMat);
      SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
      int filteredEigenPairs = filter.filter(eigenPairs.eigenValues());

      // correlationDimension = #strong EV
      int thisdim = filteredEigenPairs;

      // FIXME: handle the case of no strong EVs.
      assert ((thisdim > 0) && (thisdim <= dim));
      double thisexplain = computeExplainedVariance(eigenPairs, filteredEigenPairs);

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
            best.set(thisdim - 1, prev.get(smooth).m);
            bestk[thisdim - 1] = k - smooth;
          }
        }
        prev.removeFirst();
      }
    }
    // Try all dimensions, lowest first.
    for(int i = 0; i < dim; i++) {
      if(beststrength[i] > 0.0) {
        // If the best was the lowest or the biggest k, skip it!
        if(bestk[i] == startk + smooth) {
          continue;
        }
        if(bestk[i] == results.size() - smooth - 1) {
          continue;
        }
        Matrix covMat = best.get(i);

        // We stop at the lowest dimension that did the job for us.
        // System.err.println("Auto-k: "+bestk[i]+" dim: "+(i+1));
        return processCovarMatrix(covMat);
      }
    }
    // NOTE: if we didn't get a 'maximum' anywhere, we end up with the data from
    // the last run of the loop above. I.e. PCA on the full data set. That is
    // intended.
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Compute the explained variance for a filtered EigenPairs.
   * 
   * @param eigenPairs Eigenpairs
   * @param filteredEigenPairs Filtered eigenpairs
   * @return explained variance by the strong eigenvectors.
   */
  private double computeExplainedVariance(SortedEigenPairs eigenPairs, int filteredEigenPairs) {
    double strongsum = 0.0;
    double weaksum = 0.0;
    for (int i = 0; i < eigenPairs.size(); i++) {
      if (i <= filteredEigenPairs) {
        strongsum += eigenPairs.eigenValue(i);
      } else {
        weaksum += eigenPairs.eigenValue(i);
      }
    }
    return strongsum / (strongsum / weaksum);
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
      catch(ClassCastException|UnsupportedOperationException e) {
        LoggingUtil.warning("WARNING: results not sorted by distance!", e);
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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
