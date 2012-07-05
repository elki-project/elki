package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Performs a self-tuning local PCA based on the covariance matrices of given
 * objects. At most the closest 'k' points are used in the calculation and a
 * weight function is applied.
 * 
 * The number of points actually used depends on when the strong eigenvectors
 * exhibit the clearest correlation.
 * 
 * @author Erich Schubert
 * @param <V> vector type
 */
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "A General Framework for Increasing the Robustness of PCA-based Correlation Clustering Algorithms", booktitle = "Proceedings of the 20th International Conference on Scientific and Statistical Database Management (SSDBM), Hong Kong, China, 2008", url = "http://dx.doi.org/10.1007/978-3-540-69497-7_27")
public class PCAFilteredAutotuningRunner<V extends NumberVector<? extends V, ?>> extends PCAFilteredRunner<V> {
  /**
   * Constructor.
   * 
   * @param covarianceMatrixBuilder
   * @param eigenPairFilter
   * @param big
   * @param small
   */
  public PCAFilteredAutotuningRunner(CovarianceMatrixBuilder<V> covarianceMatrixBuilder, EigenPairFilter eigenPairFilter, double big, double small) {
    super(covarianceMatrixBuilder, eigenPairFilter, big, small);
  }

  @Override
  public PCAFilteredResult processIds(DBIDs ids, Relation<? extends V> database) {
    // Assume Euclidean distance. In the context of PCA, the neighborhood should
    // be L2-spherical to be unbiased.
    V center = DatabaseUtil.centroid(database, ids);
    DoubleDistanceDBIDList dres = new DoubleDistanceDBIDList(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double dist = EuclideanDistanceFunction.STATIC.doubleDistance(center, database.get(iter));
      dres.add(dist, iter);
    }
    dres.sort();
    return processQueryResult(dres, database);
  }

  @Override
  public <D extends NumberDistance<D, ?>> PCAFilteredResult processQueryResult(DistanceDBIDResult<D> results, Relation<? extends V> database) {
    assertSortedByDistance(results);
    final int dim = DatabaseUtil.dimensionality(database);

    List<Matrix> best = new LinkedList<Matrix>();
    for(int i = 0; i < dim; i++) {
      best.add(null);
    }
    double[] beststrength = new double[dim];
    for(int i = 0; i < dim; i++) {
      beststrength[i] = -1;
    }
    int[] bestk = new int[dim];
    // 'history'
    LinkedList<Matrix> prevM = new LinkedList<Matrix>();
    LinkedList<Double> prevS = new LinkedList<Double>();
    LinkedList<Integer> prevD = new LinkedList<Integer>();
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
      FilteredEigenPairs filteredEigenPairs = getEigenPairFilter().filter(eigenPairs);

      // correlationDimension = #strong EV
      int thisdim = filteredEigenPairs.countStrongEigenPairs();

      // FIXME: handle the case of no strong EVs.
      assert ((thisdim > 0) && (thisdim <= dim));
      double thisexplain = computeExplainedVariance(filteredEigenPairs);

      prevM.add(covMat);
      prevS.add(thisexplain);
      prevD.add(thisdim);
      assert (prevS.size() == prevM.size());
      assert (prevS.size() == prevD.size());

      if(prevS.size() >= 2 * smooth + 1) {
        // all the same dimension?
        boolean samedim = true;
        for(Iterator<Integer> it = prevD.iterator(); it.hasNext();) {
          if(it.next().intValue() != thisdim) {
            samedim = false;
          }
        }
        if(samedim) {
          // average their explain values
          double avgexplain = 0.0;
          for(Iterator<Double> it = prevS.iterator(); it.hasNext();) {
            avgexplain += it.next().doubleValue();
          }
          avgexplain /= prevS.size();

          if(avgexplain > beststrength[thisdim - 1]) {
            beststrength[thisdim - 1] = avgexplain;
            best.set(thisdim - 1, prevM.get(smooth));
            bestk[thisdim - 1] = k - smooth;
          }
        }
        prevM.removeFirst();
        prevS.removeFirst();
        prevD.removeFirst();
        assert (prevS.size() == prevM.size());
        assert (prevS.size() == prevD.size());
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
   * Compute the explained variance for a FilteredEigenPairs
   * 
   * @param filteredEigenPairs
   * @return explained variance by the strong eigenvectors.
   */
  private double computeExplainedVariance(FilteredEigenPairs filteredEigenPairs) {
    double strongsum = 0.0;
    double weaksum = 0.0;
    for(EigenPair ep : filteredEigenPairs.getStrongEigenPairs()) {
      strongsum += ep.getEigenvalue();
    }
    for(EigenPair ep : filteredEigenPairs.getWeakEigenPairs()) {
      weaksum += ep.getEigenvalue();
    }
    return strongsum / (strongsum / weaksum);
  }

  /**
   * Ensure that the results are sorted by distance.
   * 
   * @param results
   */
  private <D extends NumberDistance<D, ?>> void assertSortedByDistance(DistanceDBIDResult<D> results) {
    // TODO: sort results instead?
    double dist = -1.0;
    boolean sorted = true;
    for(DistanceDBIDResultIter<D> it = results.iter(); it.valid(); it.advance()) {
      double qr = it.getDistance().doubleValue();
      if(qr < dist) {
        sorted = false;
      }
      dist = qr;
    }
    if(!sorted) {
      try {
        results.sort();
      }
      catch(UnsupportedOperationException e) {
        System.err.println("WARNING: results not sorted by distance!");
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
  public static class Parameterizer<V extends NumberVector<? extends V, ?>> extends PCAFilteredRunner.Parameterizer<V> {
    @Override
    protected PCAFilteredAutotuningRunner<V> makeInstance() {
      return new PCAFilteredAutotuningRunner<V>(covarianceMatrixBuilder, eigenPairFilter, big, small);
    }
  }
}