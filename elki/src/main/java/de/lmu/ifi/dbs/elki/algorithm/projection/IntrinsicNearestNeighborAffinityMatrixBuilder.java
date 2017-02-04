package de.lmu.ifi.dbs.elki.algorithm.projection;
/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2016
 * Ludwig-Maximilians-Universität München
 * Lehr- und Forschungseinheit für Datenbanksysteme
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

import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Build sparse affinity matrix using the nearest neighbors only.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
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

  /**
   * Average id, for logging.
   */
  Mean m = new Mean(), m2 = new Mean();

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    DistanceQuery<T> dq = relation.getDistanceQuery(distanceFunction);
    // ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Compute desired affinities.
    // double[][] dist = buildDistanceMatrix(ids, dq);
    // DenseAffinityMatrix ma = new DenseAffinityMatrix(computePij(dist,
    // perplexity, initialScale), ids);
    m.reset();
    m2.reset();
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
    LOG.statistics(new DoubleStatistic(getClass() + ".empiric-average-id", m.getMean()));
    LOG.statistics(new DoubleStatistic(getClass() + ".adjusted-average-id", m2.getMean()));
    return mat;
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
      dist.add(d);
      ind.add(ids.getOffset(iter));
    }
    double id = estimator.estimate(dist.data, dist.size);
    m.put(id);
    double max = dist.data[dist.size - 1];
    if(max > 0) {
      double scaleexp = id * .5; // Generate squared distances.
      double scalelin = 1. / max; // Linear scaling
      for(int i = 0; i < dist.size; i++) {
        dist.data[i] = FastMath.pow(dist.data[i] * scalelin, scaleexp);
      }
      m2.put(estimator.estimate(dist.data, dist.size));
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
