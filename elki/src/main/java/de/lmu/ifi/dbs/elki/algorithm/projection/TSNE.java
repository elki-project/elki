package de.lmu.ifi.dbs.elki.algorithm.projection;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * t-Stochastic Neighbor Embedding is a projection technique designed for
 * visualization that tries to preserve the nearest neighbor structure.
 * 
 * Reference:
 * <p>
 * L.J.P. van der Maaten, G.E. Hinton<br />
 * Visualizing High-Dimensional Data Using t-SNE<br />
 * Journal of Machine Learning Research 9 2008
 * </p>
 * 
 * @author Erich Schubert
 * @author Dominik Acker
 *
 * @param <O> Object type
 */
@Reference(authors = "L.J.P. van der Maaten, G.E. Hinton", //
    title = "Visualizing High-Dimensional Data Using t-SNE", //
    booktitle = "Journal of Machine Learning Research 9 2008", //
    url = "http://www.jmlr.org/papers/v9/vandermaaten08a.html")
public class TSNE<O> extends AbstractDistanceBasedAlgorithm<O, Relation<DoubleVector>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(TSNE.class);

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
   * Minimum value for qij entries (even when duplicate)
   */
  protected static final double MIN_QIJ = 1e-12;

  /**
   * Early exaggeration factor.
   */
  protected static final double EARLY_EXAGGERATION = 4.;

  /**
   * Number of iterations to apply early exaggeration.
   */
  protected static final int EARLY_EXAGGERATION_ITERATIONS = 100;

  /**
   * Scale of the initial solution.
   */
  protected static final double INITIAL_SOLUTION_SCALE = 1e-2;

  /**
   * Minimum gain in learning rate.
   */
  protected static final double MIN_GAIN = 0.01;

  /**
   * Perplexity.
   */
  protected double perplexity;

  /**
   * Desired projection dimensionality
   */
  protected int dim;

  /**
   * Initial learning rate.
   */
  protected double learningRate;

  /**
   * Final momentum.
   */
  protected double initialMomentum, finalMomentum;

  /**
   * Maximum number of iterations.
   */
  protected int maxIterations;

  /**
   * Random generator
   */
  protected RandomFactory random;

  /**
   * Keep the original data relation.
   */
  protected boolean keep;

  /**
   * Constructor with default values.
   *
   * @param distanceFunction Distance function
   * @param dim Output dimensionality
   * @param perplexity Desired perplexity
   * @param random Random generator
   */
  public TSNE(DistanceFunction<? super O> distanceFunction, int dim, double perplexity, RandomFactory random) {
    this(distanceFunction, dim, perplexity, 0.8, 100, 300, random, true);
  }

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param dim Output dimensionality
   * @param perplexity Desired perplexity
   * @param finalMomentum Final momentum
   * @param learningRate Learning rate
   * @param maxIterations Maximum number of iterations
   * @param random Random generator
   * @param keep Keep the original data (or remove it)
   */
  public TSNE(DistanceFunction<? super O> distanceFunction, int dim, double perplexity, double finalMomentum, double learningRate, int maxIterations, RandomFactory random, boolean keep) {
    super(distanceFunction);
    this.dim = dim;
    this.perplexity = perplexity;
    this.maxIterations = maxIterations;
    this.learningRate = learningRate;
    this.initialMomentum = finalMomentum >= 0.6 ? 0.5 : (0.5 * finalMomentum);
    this.finalMomentum = finalMomentum;
    this.random = random;
    this.keep = keep;
  }

  public Relation<DoubleVector> run(Relation<O> relation) {
    DistanceQuery<O> dq = relation.getDistanceQuery(getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    double[][] pij;
    { // Compute desired affinities.
      double[][] dist = buildDistanceMatrix(size, dq, ix, iy);
      pij = computePij(dist, perplexity);
      dist = null; // No longer needed.
    }

    // Create initial solution.
    double[][] sol = randomInitialSolution(size, dim, random.getSingleThreadedRandom());
    optimizetSNE(pij, sol);

    // Transform into output data format.
    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(ix.seek(0); ix.valid(); ix.advance()) {
      proj.put(ix, new DoubleVector(sol[ix.getOffset()]));
    }

    // Remove the original (unprojected) data unless told otherwise.
    if(!keep) {
      ResultUtil.removeRecursive(relation.getHierarchy(), relation);
    }
    return new MaterializedRelation<>("tSNE", "t-SNE", otype, proj, ids);
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
  protected static double[][] buildDistanceMatrix(int size, DistanceQuery<?> dq, DBIDArrayIter ix, DBIDArrayIter iy) {
    double[][] dmat = new double[size][size];
    final boolean square = !SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction());
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix.", (size * (size - 1)) >>> 1, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(TSNE.class.getName() + ".runtime.distancematrix").begin() : null;
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
   * @param logPerp log perplexity
   * @return Affinity matrix pij
   */
  protected static double[][] computePij(double[][] dist, double perplexity) {
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
    final double scale = EARLY_EXAGGERATION / (2. * sum);
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
    double betaMin = Double.NEGATIVE_INFINITY;
    double betaMax = Double.POSITIVE_INFINITY;
    for(int tries = 0; tries < PERPLEXITY_MAXITER && Math.abs(diff) > PERPLEXITY_ERROR; ++tries) {
      if(diff > 0) {
        betaMin = beta;
        beta += (betaMax == Double.POSITIVE_INFINITY) ? beta : ((betaMax - beta) * .5);
      }
      else {
        betaMax = beta;
        beta -= (betaMin == Double.NEGATIVE_INFINITY) ? beta : ((beta - betaMin) * .5);
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
   * Generate a random initial solution.
   * 
   * @param size Data set size
   * @param dim Output dimensionality
   * @param random Random generator
   * @return Initial solution matrix
   */
  protected static double[][] randomInitialSolution(final int size, final int dim, Random random) {
    double[][] sol = new double[size][dim];
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < dim; j++) {
        sol[i][j] = random.nextGaussian() * INITIAL_SOLUTION_SCALE;
      }
    }
    return sol;
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
   * Perform the actual tSNE optimization.
   * 
   * @param pij Initial affinity matrix
   * @param sol Solution output array (preinitialized)
   */
  protected void optimizetSNE(double[][] pij, double[][] sol) {
    final int size = pij.length;
    // Meta information on each point; joined for memory locality.
    // Gradient, Momentum, and learning rate
    double[][] meta = new double[size][3 * dim];
    for(int i = 0; i < size; i++) {
      Arrays.fill(meta[i], 2 * dim, 3 * dim, 1.); // Initial learning rate
    }
    // Affinity matrix in projected space
    double[][] qij = new double[size][size];

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Iterative Optimization", maxIterations, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(TSNE.class.getName() + ".runtime.optimization").begin() : null;
    // Optimize
    for(int it = 0; it < maxIterations; it++) {
      double qij_sum = computeQij(qij, sol);
      computeGradient(pij, qij, qij_sum, sol, meta);
      updateSolution(sol, meta, it);
      if(it == EARLY_EXAGGERATION_ITERATIONS) {
        removeEarlyExaggeration(pij);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(timer != null) {
      LOG.statistics(timer.end());
    }
  }

  /**
   * Compute the qij of the solution, and the sum.
   * 
   * @param qij Qij matrix (output)
   * @param solution Solution matrix (input)
   * @return qij sum
   */
  protected static double computeQij(double[][] qij, double[][] solution) {
    double qij_sum = 0;
    for(int i = 1; i < qij.length; i++) {
      final double[] qij_i = qij[i], vi = solution[i];
      for(int j = 0; j < i; j++) {
        qij_sum += qij_i[j] = qij[j][i] = 1. / (1. + sqDist(vi, solution[j]));
      }
    }
    return qij_sum * 2; // Symmetry
  }

  /**
   * Squared distance, in projection space.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return Squared distance
   */
  protected static double sqDist(double[] v1, double[] v2) {
    assert (v1.length == v2.length) : "Lengths do not agree: " + v1.length + " " + v2.length;
    double sum = 0;
    for(int i = 0; i < v1.length; i++) {
      final double diff = v1[i] - v2[i];
      sum += diff * diff;
    }
    return sum;
  }

  /**
   * Compute the gradients.
   * 
   * @param pij Desired affinity matrix
   * @param qij Projected affinity matrix
   * @param qij_sum Normalization factor
   * @param sol Current solution coordinates
   * @param meta Point metadata
   */
  protected void computeGradient(double[][] pij, double[][] qij, double qij_sum, double[][] sol, double[][] meta) {
    for(int i = 0; i < pij.length; i++) {
      final double[] sol_i = sol[i], meta_i = meta[i];
      final double[] pij_i = pij[i], qij_i = qij[i];
      Arrays.fill(meta_i, 0, dim, 0.); // Clear gradient only
      for(int j = 0; j < pij.length; j++) {
        if(i == j) {
          continue;
        }
        final double[] sol_j = sol[j];
        final double qij_ij = qij_i[j];
        // Qij after scaling!
        final double q = MathUtil.max(qij_ij / qij_sum, MIN_QIJ);
        double a = (pij_i[j] - q) * qij_ij;
        for(int k = 0; k < dim; k++) {
          meta_i[k] += a * (sol_i[k] - sol_j[k]);
        }
      }
    }
  }

  /**
   * Update the current solution on iteration.
   * 
   * @param sol Solution matrix
   * @param meta Metadata array (gradient, momentum, learning rate)
   * @param it Iteration number, to choose momentum factor.
   */
  protected void updateSolution(double[][] sol, double[][] meta, int it) {
    final double mom = (it < 20 && initialMomentum < finalMomentum) ? initialMomentum : finalMomentum;
    for(int i = 0; i < sol.length; i++) {
      final double[] sol_i = sol[i], meta_i = meta[i];
      for(int k = 0; k < dim; k++) {
        // Indexes in meta array
        final int gradk = k, movk = gradk + dim, gaink = movk + dim;
        // Adjust learning rate:
        meta_i[gaink] = MathUtil.max(((meta_i[gradk] > 0) != (meta_i[movk] > 0)) ? (meta_i[gaink] + 0.2) : (meta_i[gaink] * 0.8), MIN_GAIN);
        meta_i[movk] *= mom; // Dampening the previous momentum
        meta_i[movk] -= learningRate * meta_i[gradk] * meta_i[gaink]; // Learn
        sol_i[k] += meta_i[movk];
      }
    }
  }

  /**
   * Remove the early exaggeration added before.
   * 
   * @param pij Affinity Matrix
   */
  protected static void removeEarlyExaggeration(double[][] pij) {
    final int size = pij.length;
    for(int i = 0; i < size; i++) {
      double[] row_i = pij[i];
      for(int j = 0; j < row_i.length; j++) {
        row_i[j] /= EARLY_EXAGGERATION;
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Perplexity parameter, the number of neighbors to preserve.
     */
    public static final OptionID PERPLEXITY_ID = new OptionID("tsne.perplexity", "Desired perplexity (approximately the number of neighbors to preserve)");

    /**
     * Desired projection dimensionality.
     */
    public static final OptionID DIM_ID = new OptionID("tsne.dim", "Output dimensionality.");

    /**
     * Initial momentum.
     */
    public static final OptionID MOMENTUM_ID = new OptionID("tsne.momentum", "The final momentum to use.");

    /**
     * Initial learning rate.
     */
    public static final OptionID LEARNING_RATE_ID = new OptionID("tsne.learningrate", "Learning rate of the method.");

    /**
     * Maximum number of iterations to allow.
     */
    public static final OptionID MAX_ITER_ID = new OptionID("tsne.maxiter", "Maximum number of iterations to perform.");

    /**
     * Random generator seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("tsne.seed", "Random generator seed");

    /**
     * Flag to keep the original projection
     */
    public static final OptionID KEEP_ID = new OptionID("tsne.retain-original", "Retain the original data.");

    /**
     * Desired projection dimensionality
     */
    protected int dim;

    /**
     * Perplexity.
     */
    protected double perplexity;

    /**
     * Initial learning rate.
     */
    protected double learningRate;

    /**
     * Final momentum.
     */
    protected double finalMomentum;

    /**
     * Maximum number of iterations.
     */
    protected int maxIterations;

    /**
     * Random generator
     */
    protected RandomFactory random;

    /**
     * Keep the original data relation.
     */
    protected boolean keep;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config); // Distance function

      IntParameter dimP = new IntParameter(DIM_ID) //
          .setDefaultValue(2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimP)) {
        dim = dimP.intValue();
      }

      DoubleParameter perplexityP = new DoubleParameter(PERPLEXITY_ID)//
          .setDefaultValue(40.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(perplexityP)) {
        perplexity = perplexityP.doubleValue();
      }

      DoubleParameter momentumP = new DoubleParameter(MOMENTUM_ID)//
          .setDefaultValue(0.8) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(momentumP)) {
        finalMomentum = momentumP.doubleValue();
      }

      DoubleParameter learningRateP = new DoubleParameter(LEARNING_RATE_ID)//
          .setDefaultValue(100.) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(learningRateP)) {
        learningRate = learningRateP.doubleValue();
      }

      IntParameter maxiterP = new IntParameter(MAX_ITER_ID)//
          .setDefaultValue(300)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxIterations = maxiterP.intValue();
      }

      RandomParameter randP = new RandomParameter(RANDOM_ID);
      if(config.grab(randP)) {
        random = randP.getValue();
      }

      Flag keepF = new Flag(KEEP_ID);
      if(config.grab(keepF)) {
        keep = keepF.isTrue();
      }
    }

    @Override
    protected TSNE<O> makeInstance() {
      return new TSNE<>(distanceFunction, dim, perplexity, finalMomentum, learningRate, maxIterations, random, keep);
    }
  }
}
