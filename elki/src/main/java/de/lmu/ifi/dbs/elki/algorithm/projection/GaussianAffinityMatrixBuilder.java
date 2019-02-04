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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Compute the affinity matrix for SNE and tSNE using a Gaussian distribution
 * with a constant sigma.
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
public class GaussianAffinityMatrixBuilder<O> implements AffinityMatrixBuilder<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GaussianAffinityMatrixBuilder.class);

  /**
   * Minimum value for pij entries (even when duplicate)
   */
  protected static final double MIN_PIJ = 1e-12;

  /**
   * Input distance function.
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Kernel bandwidth sigma.
   */
  protected double sigma;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param sigma Gaussian kernel bandwidth
   */
  public GaussianAffinityMatrixBuilder(DistanceFunction<? super O> distanceFunction, double sigma) {
    super();
    this.distanceFunction = distanceFunction;
    this.sigma = sigma;
  }

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    DistanceQuery<T> dq = relation.getDistanceQuery(distanceFunction);
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Compute desired affinities.
    double[][] dist = buildDistanceMatrix(ids, dq);
    return new DenseAffinityMatrix(computePij(dist, sigma, initialScale), ids);
  }

  /**
   * Build a distance matrix of squared distances.
   *
   * @param ids DBIDs
   * @param dq Distance query
   * @return Distance matrix
   */
  protected double[][] buildDistanceMatrix(ArrayDBIDs ids, DistanceQuery<?> dq) {
    final int size = ids.size();
    double[][] dmat = new double[size][size];
    final boolean square = !dq.getDistanceFunction().isSquared();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix", (size * (size - 1)) >>> 1, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".runtime.distancematrix").begin() : null;
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    for(ix.seek(0); ix.valid(); ix.advance()) {
      double[] dmat_x = dmat[ix.getOffset()];
      for(iy.seek(ix.getOffset() + 1); iy.valid(); iy.advance()) {
        final double dist = dq.distance(ix, iy);
        dmat[iy.getOffset()][ix.getOffset()] = dmat_x[iy.getOffset()] = square ? (dist * dist) : dist;
      }
      if(prog != null) {
        int row = ix.getOffset() + 1;
        prog.setProcessed(row * size - ((row * (row + 1)) >>> 1), LOG);
      }
    }
    LOG.ensureCompleted(prog);
    if(timer != null) {
      LOG.statistics(timer.end());
    }
    return dmat;
  }

  /**
   * Compute the pij from the distance matrix.
   * 
   * @param dist Distance matrix.
   * @param sigma Kernel bandwidth sigma
   * @param initialScale Initial scale
   * @return Affinity matrix pij
   */
  protected static double[][] computePij(double[][] dist, double sigma, double initialScale) {
    final int size = dist.length;
    final double msigmasq = -.5 / (sigma * sigma);
    double[][] pij = new double[size][size];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing affinities", size, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(GaussianAffinityMatrixBuilder.class.getName() + ".runtime.pijmatrix").begin() : null;
    MeanVariance mv = LOG.isStatistics() ? new MeanVariance() : null;
    for(int i = 0; i < size; i++) {
      double logP = computeH(i, dist[i], pij[i], msigmasq);
      if(mv != null) {
        mv.put(FastMath.exp(logP));
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) { // timer != null, mv != null
      LOG.statistics(timer.end());
      LOG.statistics(new DoubleStatistic(GaussianAffinityMatrixBuilder.class.getName() + ".perplexity.average", mv.getMean()));
      LOG.statistics(new DoubleStatistic(GaussianAffinityMatrixBuilder.class.getName() + ".perplexity.stddev", mv.getSampleStddev()));
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
   * Compute H (observed perplexity) for row i, and the row pij_i.
   * 
   * @param i Current point i (entry i will be ignored)
   * @param dist_i Distance matrix row (input)
   * @param pij_i Row pij[i] (output)
   * @param mbeta {@code -1. / (2 * sigma * sigma)}
   * @return Observed perplexity
   */
  protected static double computeH(final int i, double[] dist_i, double[] pij_i, double mbeta) {
    double sumP = 0.;
    // Skip point "i", break loop in two:
    for(int j = 0; j < i; j++) {
      sumP += (pij_i[j] = FastMath.exp(dist_i[j] * mbeta));
    }
    for(int j = i + 1; j < dist_i.length; j++) {
      sumP += (pij_i[j] = FastMath.exp(dist_i[j] * mbeta));
    }
    if(!(sumP > 0)) {
      // All pij are zero. Bad news.
      return Double.NEGATIVE_INFINITY;
    }
    final double s = 1. / sumP; // Scaling factor
    double sum = 0.;
    // While we could skip pi[i], it should be 0 anyway.
    for(int j = 0; j < dist_i.length; j++) {
      sum += dist_i[j] * (pij_i[j] *= s);
    }
    return FastMath.log(sumP) - mbeta * sum;
  }

  /**
   * Supported input data.
   *
   * @return Input data type information.
   */
  @Override
  public TypeInformation getInputTypeRestriction() {
    return distanceFunction.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Sigma parameter, the Gaussian bandwidth
     */
    public static final OptionID SIGMA_ID = new OptionID("sne.sigma", "Gaussian kernel standard deviation.");

    /**
     * Bandwidth.
     */
    protected double sigma;

    @Override
    protected void makeOptions(Parameterization config) {
      // Override: super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
      DoubleParameter sigmaP = new DoubleParameter(SIGMA_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }
    }

    @Override
    protected GaussianAffinityMatrixBuilder<O> makeInstance() {
      return new GaussianAffinityMatrixBuilder<>(distanceFunction, sigma);
    }
  }
}
