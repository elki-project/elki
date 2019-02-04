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
package de.lmu.ifi.dbs.elki.algorithm.outlier.subspace;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.KernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Adaptive outlierness for subspace outlier ranking (OUTRES).
 * <p>
 * Note: this algorithm seems to have a O(n³d!) complexity with no obvious way
 * to accelerate it with usual index structures for range queries: each object
 * in each tested subspace will need to know the mean and standard deviation of
 * the density of the neighbors, which in turn needs another range query; except
 * if we precomputed the densities for each of O(d!) possible subsets of
 * dimensions.
 * <p>
 * Reference:
 * <p>
 * E. Müller, M. Schiffer, T. Seidl<br>
 * Adaptive outlierness for subspace outlier ranking<br>
 * Proc. 19th ACM Int. Conf. on Information and Knowledge Management
 *
 * @author Viktoria Pleintinger
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - KernelDensityEstimator
 */
@Reference(authors = "E. Müller, M. Schiffer, T. Seidl", //
    title = "Adaptive outlierness for subspace outlier ranking", //
    booktitle = "Proc. 19th ACM Int. Conf. on Information and Knowledge Management", //
    url = "https://doi.org/10.1145/1871437.1871690", //
    bibkey = "DBLP:conf/cikm/MullerSS10")
public class OUTRES extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OUTRES.class);

  /**
   * The epsilon (in 2d) parameter
   */
  private final double eps;

  /**
   * Constant for Kolmogorov-Smirnov at alpha=0.01 (table value)
   */
  private static final double K_S_CRITICAL001 = 1.63;

  /**
   * Constructor.
   * 
   * @param eps Epsilon
   */
  public OUTRES(double eps) {
    super();
    this.eps = eps;
  }

  /**
   * Main loop for OUTRES
   * 
   * @param relation Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<? extends NumberVector> relation) {
    final DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore ranks = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    KernelDensityEstimator kernel = new KernelDensityEstimator(relation, eps);
    long[] subspace = BitsUtil.zero(kernel.dim);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("OUTRES scores", ids.size(), LOG) : null;

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      BitsUtil.zeroI(subspace);
      double score = outresScore(0, subspace, iditer, kernel, ids);
      ranks.putDouble(iditer, score);
      minmax.put(score);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);

    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., 1., 1.);
    return new OutlierResult(meta, new MaterializedDoubleRelation("OUTRES", "outres-score", ranks, ids));
  }

  /**
   * Main loop of OUTRES. Run for each object
   * 
   * @param s start dimension
   * @param subspace Current subspace
   * @param id Current object ID
   * @param kernel Kernel
   * @param cands neighbor candidates
   * @return Score
   */
  public double outresScore(final int s, long[] subspace, DBIDRef id, KernelDensityEstimator kernel, DBIDs cands) {
    double score = 1.0; // Initial score is 1.0
    final SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(subspace);
    MeanVariance meanv = new MeanVariance();
    ModifiableDoubleDBIDList neighcand = DBIDUtil.newDistanceDBIDList(cands.size());
    ModifiableDoubleDBIDList nn = DBIDUtil.newDistanceDBIDList(cands.size());

    for(int i = s; i < kernel.dim; i++) {
      assert !BitsUtil.get(subspace, i);
      BitsUtil.setI(subspace, i);
      df.setSelectedDimensions(subspace);
      final double adjustedEps = kernel.adjustedEps(kernel.dim);
      DoubleDBIDList neigh = initialRange(id, cands, df, adjustedEps * 2, kernel, neighcand);
      // Relevance test
      if(neigh.size() > 2) {
        if(relevantSubspace(subspace, neigh, kernel)) {
          final double density = kernel.subspaceDensity(subspace, neigh);
          // Compute mean and standard deviation for densities of neighbors.
          meanv.reset();
          for(DoubleDBIDListIter neighbor = neigh.iter(); neighbor.valid(); neighbor.advance()) {
            subsetNeighborhoodQuery(neighcand, neighbor, df, adjustedEps, kernel, nn);
            meanv.put(kernel.subspaceDensity(subspace, nn));
          }
          final double deviation = (meanv.getMean() - density) / (2. * meanv.getSampleStddev());
          // High deviation:
          if(deviation >= 1) {
            score *= density / deviation;
          }
          // Recursion
          score *= outresScore(i + 1, subspace, id, kernel, neighcand);
        }
      }
      BitsUtil.clearI(subspace, i);
    }
    return score;
  }

  /**
   * Initial range query.
   *
   * @param obj Object
   * @param cands Candidates
   * @param df Distance function
   * @param eps Epsilon radius
   * @param kernel Kernel
   * @param n Output buffer
   * @return Neighbors
   */
  private DoubleDBIDList initialRange(DBIDRef obj, DBIDs cands, PrimitiveDistanceFunction<? super NumberVector> df, double eps, KernelDensityEstimator kernel, ModifiableDoubleDBIDList n) {
    n.clear();
    NumberVector o = kernel.relation.get(obj);
    final double twoeps = eps * 2;
    int matches = 0;
    for(DBIDIter cand = cands.iter(); cand.valid(); cand.advance()) {
      final double dist = df.distance(o, kernel.relation.get(cand));
      if(dist <= twoeps) {
        n.add(dist, cand);
        if(dist <= eps) {
          ++matches;
        }
      }
    }
    n.sort();
    return n.slice(0, matches);
  }

  /**
   * Refine neighbors within a subset.
   * 
   * @param neighc Neighbor candidates
   * @param dbid Query object
   * @param df distance function
   * @param adjustedEps Epsilon range
   * @param kernel Kernel
   * @param n Output list
   * @return Neighbors of neighbor object
   */
  private DoubleDBIDList subsetNeighborhoodQuery(DoubleDBIDList neighc, DBIDRef dbid, PrimitiveDistanceFunction<? super NumberVector> df, double adjustedEps, KernelDensityEstimator kernel, ModifiableDoubleDBIDList n) {
    n.clear();
    NumberVector query = kernel.relation.get(dbid);
    for(DoubleDBIDListIter neighbor = neighc.iter(); neighbor.valid(); neighbor.advance()) {
      // TODO: use triangle inequality for pruning
      double dist = df.distance(query, kernel.relation.get(neighbor));
      if(dist <= adjustedEps) {
        n.add(dist, neighbor);
      }
    }
    return n;
  }

  /**
   * Subspace relevance test.
   * 
   * @param subspace Subspace to test
   * @param neigh Neighbor list
   * @param kernel Kernel density estimator
   * @return relevance test result
   */
  protected boolean relevantSubspace(long[] subspace, DoubleDBIDList neigh, KernelDensityEstimator kernel) {
    final double crit = K_S_CRITICAL001 / FastMath.sqrt(neigh.size() - 2);

    double[] data = new double[neigh.size()];
    Relation<? extends NumberVector> relation = kernel.relation;
    for(int dim = BitsUtil.nextSetBit(subspace, 0); dim >= 0; dim = BitsUtil.nextSetBit(subspace, dim + 1)) {
      // TODO: can/should we save this copy?
      int count = 0;
      for(DBIDIter neighbor = neigh.iter(); neighbor.valid(); neighbor.advance()) {
        data[count++] = relation.get(neighbor).doubleValue(dim);
      }
      assert (count == neigh.size());
      Arrays.sort(data);

      final double min = data[0], norm = data[data.length - 1] - min;
      // Kolmogorow-Smirnow-Test against uniform distribution:
      boolean flag = false;
      for(int j = 1, end = data.length - 1; j < end; j++) {
        if(Math.abs(j / (data.length - 2.) - (data[j] - min) / norm) > crit) {
          flag = true;
          break;
        }
      }
      if(!flag) {
        return false;
      }
    }
    return true;
  }

  /**
   * Kernel density estimation and utility class.
   * 
   * @author Erich Schubert
   */
  protected static class KernelDensityEstimator {
    /**
     * Actual kernel in use
     */
    final KernelDensityFunction kernel = EpanechnikovKernelDensityFunction.KERNEL;

    /**
     * Relation to retrieve data from
     */
    final Relation<? extends NumberVector> relation;

    /**
     * Epsilon values for different subspace dimensionalities
     */
    final double[] epsilons;

    /**
     * Optimal bandwidth for a dimensionality of 2
     */
    final double hopttwo;

    /**
     * Dimensionality of data set
     */
    final int dim;

    /**
     * Constructor.
     * 
     * @param relation Relation to apply to
     */
    public KernelDensityEstimator(Relation<? extends NumberVector> relation, double eps) {
      super();
      this.relation = relation;
      dim = RelationUtil.dimensionality(relation);
      hopttwo = optimalBandwidth(2);
      epsilons = new double[dim + 1];
      Arrays.fill(epsilons, Double.NEGATIVE_INFINITY);
      epsilons[2] = eps;
    }

    /**
     * Compute density in the given subspace.
     * 
     * @param subspace Subspace
     * @param neighbors Neighbor distance list
     * @return Density
     */
    protected double subspaceDensity(long[] subspace, DoubleDBIDList neighbors) {
      final double bandwidth = optimalBandwidth(BitsUtil.cardinality(subspace));
      double density = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        double v = neighbor.doubleValue() / bandwidth;
        density += v < 1 ? 1 - (v * v) : 0;
      }
      return density / relation.size();
    }

    /**
     * Compute optimal kernel bandwidth
     * 
     * @param dim Dimensionality of subspace
     * @return optimal bandwidth
     */
    protected double optimalBandwidth(int dim) {
      // Pi in the publication is redundant and cancels out!
      double hopt = 8 * GammaDistribution.gamma(dim / 2.0 + 1) * (dim + 4) * MathUtil.powi(2, dim);
      return hopt * FastMath.pow(relation.size(), (-1. / (dim + 4)));
    }

    /**
     * Rescale the query radius based on the given dimensionality.
     * 
     * @param dim Dimensionality
     * @return Query radius
     */
    protected double adjustedEps(int dim) {
      // Cached
      double e = epsilons[dim];
      if(e < 0) {
        epsilons[dim] = e = epsilons[2] * optimalBandwidth(dim) / hopttwo;
      }
      return e;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Viktoria Pleintinger
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for Epsilon parameter
     */
    public static final OptionID D_ID = new OptionID("outres.epsilon", "Range value for OUTRES in 2 dimensions.");

    /**
     * Query radius
     */
    protected double eps;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter param = new DoubleParameter(D_ID);
      if(config.grab(param)) {
        eps = param.getValue();
      }
    }

    @Override
    protected OUTRES makeInstance() {
      return new OUTRES(eps);
    }
  }
}
