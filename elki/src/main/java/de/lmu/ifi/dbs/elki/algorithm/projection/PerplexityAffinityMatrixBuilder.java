package de.lmu.ifi.dbs.elki.algorithm.projection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
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
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the affinity matrix for SNE and tSNE.
 * 
 * @author Erich Schubert
 *
 * @param <O> Vector type
 */
public class PerplexityAffinityMatrixBuilder<O> implements AffinityMatrixBuilder<O> {
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
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Perplexity.
   */
  protected double perplexity;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param perplexity Perplexity
   */
  public PerplexityAffinityMatrixBuilder(DistanceFunction<? super O> distanceFunction, double perplexity) {
    super();
    this.distanceFunction = distanceFunction;
    this.perplexity = perplexity;
  }

  @Override
  public <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale) {
    DistanceQuery<T> dq = relation.getDistanceQuery(distanceFunction);
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Compute desired affinities.
    double[][] dist = buildDistanceMatrix(ids, dq);
    return new DenseAffinityMatrix(computePij(dist, perplexity, initialScale), ids);
  }

  /**
   * Build a distance matrix of squared distances.
   * 
   * @param size Data set size
   * @param dq Distance query
   * @param ix Data iterator
   * @param iy Data iterator
   * @return Distance matrix
   */
  private double[][] buildDistanceMatrix(ArrayDBIDs ids, DistanceQuery<?> dq) {
    final int size = ids.size();
    double[][] dmat = new double[size][size];
    final boolean square = !SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction());
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix", (size * (size - 1)) >>> 1, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(PerplexityAffinityMatrixBuilder.class.getName() + ".runtime.distancematrix").begin() : null;
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
   * @param perplexity Desired perplexity
   * @param initialScale Initial scale
   * @return Affinity matrix pij
   */
  protected static double[][] computePij(double[][] dist, double perplexity, double initialScale) {
    final int size = dist.length;
    final double logPerp = Math.log(perplexity);
    double[][] pij = new double[size][size];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Optimizing perplexities", size, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(TSNE.class.getName() + ".runtime.pijmatrix").begin() : null;
    for(int i = 0; i < size; i++) {
      computePi(i, dist[i], pij[i], perplexity, logPerp);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(timer != null) {
      LOG.statistics(timer.end());
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
   */
  protected static void computePi(int i, double[] dist_i, double[] pij_i, double perplexity, double logPerp) {
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
  }

  /**
   * Estimate beta from the distances in a row.
   * 
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
      sum += d < Double.POSITIVE_INFINITY ? d : 0.;
    }
    // TODO: fail gracefully if all distances are zero.
    assert (sum > 0. && sum < Double.POSITIVE_INFINITY);
    return .5 / sum * perplexity * (dist_i.length - 1.);
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
      sumP += (pij_i[j] = Math.exp(dist_i[j] * mbeta));
    }
    for(int j = i + 1; j < dist_i.length; j++) {
      sumP += (pij_i[j] = Math.exp(dist_i[j] * mbeta));
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
    return Math.log(sumP) - mbeta * sum;
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
     * Perplexity parameter, the number of neighbors to preserve.
     */
    public static final OptionID PERPLEXITY_ID = new OptionID("sne.perplexity", "Desired perplexity (approximately the number of neighbors to preserve)");

    /**
     * Perplexity.
     */
    protected double perplexity;

    @Override
    protected void makeOptions(Parameterization config) {
      // Override: super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = AbstractDistanceBasedAlgorithm.makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
      DoubleParameter perplexityP = new DoubleParameter(PERPLEXITY_ID)//
          .setDefaultValue(40.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(perplexityP)) {
        perplexity = perplexityP.doubleValue();
      }
    }

    @Override
    protected PerplexityAffinityMatrixBuilder<O> makeInstance() {
      return new PerplexityAffinityMatrixBuilder<>(distanceFunction, perplexity);
    }
  }
}
