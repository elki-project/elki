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
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import net.jafama.FastMath;

/**
 * Build sparse affinity matrix using the nearest neighbors only.
 * <p>
 * Reference:
 * <p>
 * L. J. P. van der Maaten<br>
 * Accelerating t-SNE using Tree-Based Algorithms<br>
 * Journal of Machine Learning Research 15
 * <p>
 * TODO: this implementation currently differs in one major point: we do not
 * symmetrize the sparse \(p_{ij}\) matrix.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type
 */
@Reference(authors = "L. J. P. van der Maaten", //
    title = "Accelerating t-SNE using Tree-Based Algorithms", //
    booktitle = "Journal of Machine Learning Research 15", //
    url = "http://dl.acm.org/citation.cfm?id=2697068", //
    bibkey = "DBLP:journals/jmlr/Maaten14")
public class NearestNeighborAffinityMatrixBuilder<O> extends PerplexityAffinityMatrixBuilder<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NearestNeighborAffinityMatrixBuilder.class);

  /**
   * Number of neighbors to use.
   */
  protected int numberOfNeighbours;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param perplexity Desired perplexity (will use 3*perplexity neighbors)
   */
  public NearestNeighborAffinityMatrixBuilder(DistanceFunction<? super O> distanceFunction, double perplexity) {
    super(distanceFunction, perplexity);
    this.numberOfNeighbours = (int) FastMath.ceil(3 * perplexity);
  }

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param perplexity Desired perplexity
   * @param neighbors Number of neighbors to use
   */
  public NearestNeighborAffinityMatrixBuilder(DistanceFunction<? super O> distanceFunction, double perplexity, int neighbors) {
    super(distanceFunction, perplexity);
    this.numberOfNeighbours = neighbors;
  }

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    DistanceQuery<T> dq = relation.getDistanceQuery(distanceFunction);
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
    final boolean square = !dq.getDistanceFunction().isSquared();
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
    for(DBIDArrayIter ix = ids.iter(); ix.valid(); ix.advance()) {
      dists.clear();
      inds.clear();
      KNNList neighbours = knnq.getKNNForDBID(ix, numberOfNeighbours + 1);
      convertNeighbors(ids, ix, square, neighbours, dists, inds);
      double beta = computeSigma(ix.getOffset(), dists, perplexity, logPerp, //
          pij[ix.getOffset()] = new double[dists.size()]);
      if(mv != null) {
        mv.put(beta > 0 ? FastMath.sqrt(.5 / beta) : 0.); // Sigma
      }
      indices[ix.getOffset()] = inds.toArray();
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    // Sum of the sparse affinity matrix:
    double sum = 0.;
    for(int i = 0; i < pij.length; i++) {
      final double[] pij_i = pij[i];
      for(int j = 0; j < pij_i.length; j++) {
        sum += pij_i[j];
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
            final double val = pij_i[offi] + pij[j][offj]; // Symmetrize
            pij_i[offi] = pij[j][offj] = MathUtil.max(val * scale, MIN_PIJ);
          }
        }
        else { // Not found
          // TODO: the original code produces a symmetric matrix
          // And it will now not sum to EARLY_EXAGGERATION anymore.
          pij_i[offi] = MathUtil.max(pij_i[offi] * scale, MIN_PIJ);
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
   */
  protected void convertNeighbors(DBIDRange ids, DBIDRef ix, boolean square, KNNList neighbours, DoubleArray dist, IntegerArray ind) {
    for(DoubleDBIDListIter iter = neighbours.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, ix)) {
        continue; // Skip query point
      }
      double d = iter.doubleValue();
      dist.add(square ? (d * d) : d);
      ind.add(ids.getOffset(iter));
    }
  }

  /**
   * Compute row pij[i], using binary search on the kernel bandwidth sigma to
   * obtain the desired perplexity.
   *
   * @param i Current point
   * @param pij_row Distance matrix row pij[i]
   * @param perplexity Desired perplexity
   * @param log_perp Log of desired perplexity
   * @param pij_i Output row
   * @return beta
   */
  protected static double computeSigma(int i, DoubleArray pij_row, double perplexity, double log_perp, double[] pij_i) {
    double max = pij_row.get((int) FastMath.ceil(perplexity)) / Math.E;
    double beta = 1 / max; // beta = 1. / (2*sigma*sigma)
    double diff = computeH(pij_row, pij_i, -beta) - log_perp;
    double betaMin = 0.;
    double betaMax = Double.POSITIVE_INFINITY;
    for(int tries = 0; tries < PERPLEXITY_MAXITER && Math.abs(diff) > PERPLEXITY_ERROR; ++tries) {
      if(diff > 0) {
        betaMin = beta;
        beta += (betaMax == Double.POSITIVE_INFINITY) ? beta : ((betaMax - beta) * .5);
      }
      else {
        betaMax = beta;
        beta -= (beta - betaMin) * .5;
      }
      diff = computeH(pij_row, pij_i, -beta) - log_perp;
    }
    return beta;
  }

  /**
   * Compute H (observed perplexity) for row i, and the row pij_i.
   * 
   * @param dist_i Distances to neighbors
   * @param pij_row Row pij[i] (output)
   * @param mbeta {@code -1. / (2 * sigma * sigma)}
   * @return Observed perplexity
   */
  protected static double computeH(DoubleArray dist_i, double[] pij_row, double mbeta) {
    final int len = dist_i.size();
    assert (pij_row.length == len);
    double sumP = 0.;
    for(int j = 0; j < len; j++) {
      sumP += (pij_row[j] = FastMath.exp(dist_i.get(j) * mbeta));
    }
    if(!(sumP > 0)) {
      // All pij are zero. Bad news.
      return Double.NEGATIVE_INFINITY;
    }
    final double s = 1. / sumP; // Scaling factor
    double sum = 0.;
    // While we could skip pi[i], it should be 0 anyway.
    for(int j = 0; j < len; j++) {
      sum += dist_i.get(j) * (pij_row[j] *= s);
    }
    return FastMath.log(sumP) - mbeta * sum;
  }

  /**
   * Check if the index array contains {@code i}.
   * 
   * TODO: sort arrays, use binary search!
   * 
   * @param is Array to search
   * @param i Index to search
   * @return Position of index i, or {@code -1} if not found.
   */
  protected static int containsIndex(int[] is, int i) {
    for(int j = 0; j < is.length; j++) {
      if(i == is[j]) {
        return j;
      }
    }
    return -1;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends PerplexityAffinityMatrixBuilder.Parameterizer<O> {
    @Override
    protected NearestNeighborAffinityMatrixBuilder<O> makeInstance() {
      return new NearestNeighborAffinityMatrixBuilder<>(distanceFunction, perplexity);
    }
  }
}
