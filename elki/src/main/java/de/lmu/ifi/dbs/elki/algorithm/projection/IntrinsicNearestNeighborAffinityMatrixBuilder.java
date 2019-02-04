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
package de.lmu.ifi.dbs.elki.algorithm.projection;

import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Build sparse affinity matrix using the nearest neighbors only, adjusting for
 * intrinsic dimensionality. On data sets with high intrinsic dimensionality,
 * this can give better results.
 * <p>
 * Furthermore, this approach uses a different rule to combine affinities:
 * rather than taking the arithmetic average of \(p_{ij}\) and \(p_{ji}\), we
 * use \(\sqrt{p_{ij} \cdot p_{ji}}\), which prevents outliers from attaching
 * closely to nearby clusters.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier
 * Detection: A Remedy Against the Curse of Dimensionality?<br>
 * Proc. Int. Conf. Similarity Search and Applications, SISAP 2017
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type
 */
@Title("Intrinsic t-Stochastic Neighbor Embedding")
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier Detection: A Remedy Against the Curse of Dimensionality?", //
    booktitle = "Proc. Int. Conf. Similarity Search and Applications, SISAP 2017", //
    url = "https://doi.org/10.1007/978-3-319-68474-1_13", //
    bibkey = "DBLP:conf/sisap/SchubertG17")
public class IntrinsicNearestNeighborAffinityMatrixBuilder<O> extends NearestNeighborAffinityMatrixBuilder<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(IntrinsicNearestNeighborAffinityMatrixBuilder.class);

  /**
   * Estimator of intrinsic dimensionality.
   */
  IntrinsicDimensionalityEstimator estimator;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param perplexity Perplexity
   * @param estimator Estimator of intrinsic dimensionality
   */
  public IntrinsicNearestNeighborAffinityMatrixBuilder(DistanceFunction<? super O> distanceFunction, double perplexity, IntrinsicDimensionalityEstimator estimator) {
    super(distanceFunction, perplexity);
    this.estimator = estimator;
  }

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    DistanceQuery<T> dq = relation.getDistanceQuery(distanceFunction);
    final int numberOfNeighbours = (int) FastMath.ceil(3 * perplexity);
    KNNQuery<T> knnq = relation.getKNNQuery(dq, numberOfNeighbours + 1);
    if(knnq instanceof LinearScanQuery && numberOfNeighbours * numberOfNeighbours < relation.size()) {
      LOG.warning("To accelerate Barnes-Hut tSNE, please use an index.");
    }
    if(!(relation.getDBIDs() instanceof DBIDRange)) {
      throw new AbortException("Distance matrixes are currently only supported for DBID ranges (as used by static databases) for performance reasons (Patches welcome).");
    }
    DBIDRange rids = (DBIDRange) relation.getDBIDs();
    final int size = rids.size();
    // Sparse affinity graph
    double[][] pij = new double[size][];
    int[][] indices = new int[size][];
    final boolean square = !SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction());
    computePij(rids, knnq, square, numberOfNeighbours, pij, indices, initialScale);
    SparseAffinityMatrix mat = new SparseAffinityMatrix(pij, indices, rids);
    return mat;
  }

  /**
   * Compute the sparse pij using the nearest neighbors only.
   * 
   * @param ids ID range
   * @param knnq kNN query
   * @param square Use squared distances
   * @param numberOfNeighbours Number of neighbors to get
   * @param pij Output of distances
   * @param indices Output of indexes
   * @param initialScale Initial scaling factor
   */
  protected void computePij(DBIDRange ids, KNNQuery<?> knnq, boolean square, int numberOfNeighbours, double[][] pij, int[][] indices, double initialScale) {
    Duration timer = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".runtime.neighborspijmatrix").begin() : null;
    final double logPerp = FastMath.log(perplexity);
    // Scratch arrays, resizable
    DoubleArray dists = new DoubleArray(numberOfNeighbours + 10);
    IntegerArray inds = new IntegerArray(numberOfNeighbours + 10);
    // Compute nearest-neighbor sparse affinity matrix
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Finding neighbors and optimizing perplexity", ids.size(), LOG) : null;
    MeanVariance mv = LOG.isStatistics() ? new MeanVariance() : null;
    Mean mid = LOG.isStatistics() ? new Mean() : null;
    for(DBIDArrayIter ix = ids.iter(); ix.valid(); ix.advance()) {
      dists.clear();
      inds.clear();
      KNNList neighbours = knnq.getKNNForDBID(ix, numberOfNeighbours + 1);
      convertNeighbors(ids, ix, square, neighbours, dists, inds, mid);
      double beta = computeSigma(ix.getOffset(), dists, perplexity, logPerp, //
          pij[ix.getOffset()] = new double[dists.size()]);
      if(mv != null) {
        mv.put(beta > 0 ? FastMath.sqrt(.5 / beta) : 0.); // Sigma
      }
      indices[ix.getOffset()] = inds.toArray();
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(mid != null) {
      LOG.statistics(new DoubleStatistic(getClass() + ".average-original-id", mid.getMean()));
    }
    // Sum of the sparse affinity matrix:
    double sum = 0.;
    for(int i = 0; i < pij.length; i++) {
      final double[] pij_i = pij[i];
      for(int offi = 0; offi < pij_i.length; offi++) {
        int j = indices[i][offi];
        if(j > i) {
          continue; // Exploit symmetry.
        }
        assert (i != j);
        int offj = containsIndex(indices[j], i);
        if(offj >= 0) { // Found
          sum += FastMath.sqrt(pij_i[offi] * pij[j][offj]);
        }
      }
    }
    final double scale = initialScale / (2 * sum);
    for(int i = 0; i < pij.length; i++) {
      final double[] pij_i = pij[i];
      for(int offi = 0; offi < pij_i.length; offi++) {
        int j = indices[i][offi];
        assert (i != j);
        int offj = containsIndex(indices[j], i);
        if(offj >= 0) { // Found
          assert (indices[j][offj] == i);
          // Exploit symmetry:
          if(i < j) {
            final double val = FastMath.sqrt(pij_i[offi] * pij[j][offj]); // Symmetrize
            pij_i[offi] = pij[j][offj] = MathUtil.max(val * scale, MIN_PIJ);
          }
        }
        else { // Not found, so zero.
          pij_i[offi] = 0;
        }
      }
    }
    if(LOG.isStatistics()) { // timer != null, mv != null
      LOG.statistics(timer.end());
      LOG.statistics(new DoubleStatistic(NearestNeighborAffinityMatrixBuilder.class.getName() + ".sigma.average", mv.getMean()));
      LOG.statistics(new DoubleStatistic(NearestNeighborAffinityMatrixBuilder.class.getName() + ".sigma.stddev", mv.getSampleStddev()));
    }
  }

  /**
   * Load a neighbor query result into a double and and integer array, also
   * removing the query point. This is necessary, because we have to modify the
   * distances.
   * 
   * TODO: sort by index, not distance
   *
   * @param ids Indexes
   * @param ix Current Object
   * @param square Use squared distances
   * @param neighbours Neighbor list
   * @param dist Output distance array
   * @param ind Output index array
   * @param m Mean id, for statistics.
   */
  protected void convertNeighbors(DBIDRange ids, DBIDRef ix, boolean square, KNNList neighbours, DoubleArray dist, IntegerArray ind, Mean m) {
    for(DoubleDBIDListIter iter = neighbours.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, ix)) {
        continue; // Skip query point
      }
      dist.add(iter.doubleValue());
      ind.add(ids.getOffset(iter));
    }
    double id = estimator.estimate(dist.data, dist.size);
    if(m != null) {
      m.put(id);
    }
    double max = dist.data[dist.size - 1];
    if(max > 0) {
      double scaleexp = id * .5; // Generate squared distances.
      double scalelin = 1. / max; // Linear scaling
      for(int i = 0; i < dist.size; i++) {
        dist.data[i] = FastMath.pow(dist.data[i] * scalelin, scaleexp);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends NearestNeighborAffinityMatrixBuilder.Parameterizer<O> {
    /**
     * Parameter for ID estimation.
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("itsne.estimator", "Estimator for intrinsic dimensionality.");

    /**
     * Estimator of intrinsic dimensionality.
     */
    IntrinsicDimensionalityEstimator estimator = AggregatedHillEstimator.STATIC;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<IntrinsicDimensionalityEstimator> estimatorP = new ObjectParameter<>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, AggregatedHillEstimator.class);
      if(config.grab(estimatorP)) {
        estimator = estimatorP.instantiateClass(config);
      }
    }

    @Override
    protected IntrinsicNearestNeighborAffinityMatrixBuilder<O> makeInstance() {
      return new IntrinsicNearestNeighborAffinityMatrixBuilder<>(distanceFunction, perplexity, estimator);
    }
  }
}
