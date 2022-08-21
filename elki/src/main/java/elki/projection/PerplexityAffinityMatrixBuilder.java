/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.projection;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.math.MathUtil;
import elki.math.MeanVariance;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Compute the affinity matrix for SNE and tSNE.
 * <p>
 * Reference:
 * <p>
 * G. Hinton, S. Roweis<br>
 * Stochastic Neighbor Embedding<br>
 * Advances in Neural Information Processing Systems 15
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Vector type
 */
@Reference(authors = "G. Hinton, S. Roweis", //
    title = "Stochastic Neighbor Embedding", //
    booktitle = "Advances in Neural Information Processing Systems 15", //
    url = "http://papers.nips.cc/paper/2276-stochastic-neighbor-embedding", //
    bibkey = "DBLP:conf/nips/HintonR02")
public class PerplexityAffinityMatrixBuilder<O> extends GaussianAffinityMatrixBuilder<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PerplexityAffinityMatrixBuilder.class);

  /**
   * Threshold for optimizing perplexity.
   */
  final static protected double PERPLEXITY_ERROR = 1e-5;

  /**
   * Maximum number of iterations when optimizing perplexity.
   */
  final static protected int PERPLEXITY_MAXITER = 50;

  /**
   * Minimum value for pij entries (even when duplicate)
   */
  protected static final double MIN_PIJ = 1e-12;

  /**
   * Input distance function.
   */
  protected Distance<? super O> distance;

  /**
   * Perplexity.
   */
  protected double perplexity;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param perplexity Perplexity
   */
  public PerplexityAffinityMatrixBuilder(Distance<? super O> distance, double perplexity) {
    super(distance, Double.NaN);
    this.distance = distance;
    this.perplexity = perplexity;
  }

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Compute desired affinities.
    double[][] dist = buildDistanceMatrix(ids, new QueryBuilder<>(relation, distance).distanceQuery());
    return new DenseAffinityMatrix(computePij(dist, perplexity, initialScale), ids);
  }

  /**
   * Compute the pij from the distance matrix.
   * 
   * @param dist Distance matrix.
   * @param perplexity Desired perplexity
   * @param initialScale Initial scale
   * @return Affinity matrix pij
   */
  protected static double[][] computePij(double[][] dist, double perplexity, double initialScale) {
    final int size = dist.length;
    final double logPerp = FastMath.log(perplexity);
    double[][] pij = new double[size][size];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Optimizing perplexities", size, LOG) : null;
    Duration timer = LOG.newDuration(PerplexityAffinityMatrixBuilder.class.getName() + ".runtime.pijmatrix").begin();
    MeanVariance mv = LOG.isStatistics() ? new MeanVariance() : null;
    for(int i = 0; i < size; i++) {
      double beta = computePi(i, dist[i], pij[i], perplexity, logPerp);
      if(mv != null) {
        mv.put(beta > 0 ? Math.sqrt(.5 / beta) : 0.); // Sigma
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    LOG.statistics(timer.end());
    if(mv != null && LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(PerplexityAffinityMatrixBuilder.class.getName() + ".sigma.average", mv.getMean()));
      LOG.statistics(new DoubleStatistic(PerplexityAffinityMatrixBuilder.class.getName() + ".sigma.stddev", mv.getSampleStddev()));
    }
    // Scale pij to have the desired sum EARLY_EXAGGERATION
    double sum = 0.;
    for(int i = 1; i < size; i++) {
      final double[] pij_i = pij[i];
      for(int j = 0; j < i; j++) { // Nur über halbe Matrix!
        sum += (pij_i[j] += pij[j][i]); // Symmetrie herstellen
      }
    }
    // Scaling taken from original tSNE code:
    final double scale = initialScale / (2. * sum);
    for(int i = 1; i < size; i++) {
      final double[] pij_i = pij[i];
      for(int j = 0; j < i; j++) {
        pij_i[j] = pij[j][i] = MathUtil.max(pij_i[j] * scale, MIN_PIJ);
      }
    }
    return pij;
  }

  /**
   * Compute row pij[i], using binary search on the kernel bandwidth sigma to
   * obtain the desired perplexity.
   *
   * @param i Current point
   * @param dist_i Distance matrix row pij[i]
   * @param pij_i Output row
   * @param perplexity Desired perplexity
   * @param logPerp Log of desired perplexity
   * @return Beta
   */
  protected static double computePi(int i, double[] dist_i, double[] pij_i, double perplexity, double logPerp) {
    // Relation to paper: beta == 1. / (2*sigma*sigma)
    double beta = estimateInitialBeta(dist_i, perplexity);
    double diff = computeH(i, dist_i, pij_i, -beta) - logPerp;
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
      diff = computeH(i, dist_i, pij_i, -beta) - logPerp;
    }
    return beta;
  }

  /**
   * Estimate beta from the distances in a row.
   * <p>
   * This lacks a mathematical argument, but is a handcrafted heuristic to avoid
   * numerical problems. The average distance is usually too large, so we scale
   * the average distance by 2*N/perplexity. Then estimate beta as 1/x.
   *
   * @param dist_i Distances
   * @param perplexity Desired perplexity
   * @return Estimated beta.
   */
  protected static double estimateInitialBeta(double[] dist_i, double perplexity) {
    double sum = 0.;
    for(double d : dist_i) {
      double d2 = d * d;
      sum += d2 < Double.POSITIVE_INFINITY ? d2 : 0.;
    }
    return sum > 0 && sum < Double.POSITIVE_INFINITY ? .5 / sum * perplexity * (dist_i.length - 1.) : 1.;
  }

  /**
   * Supported input data.
   *
   * @return Input data type information.
   */
  @Override
  public TypeInformation getInputTypeRestriction() {
    return distance.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Perplexity parameter, the number of neighbors to preserve.
     */
    public static final OptionID PERPLEXITY_ID = new OptionID("sne.perplexity", "Desired perplexity (approximately the number of neighbors to preserve)");

    /**
     * Perplexity.
     */
    protected double perplexity;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(PERPLEXITY_ID)//
          .setDefaultValue(40.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> perplexity = x);
    }

    @Override
    public PerplexityAffinityMatrixBuilder<O> make() {
      return new PerplexityAffinityMatrixBuilder<>(distance, perplexity);
    }
  }
}
