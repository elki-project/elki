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

import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Stochastic Neighbor Embedding is a projection technique designed for
 * visualization that tries to preserve the nearest neighbor structure.
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
 * @composed - - - AffinityMatrixBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "G. Hinton, S. Roweis", //
    title = "Stochastic Neighbor Embedding", //
    booktitle = "Advances in Neural Information Processing Systems 15", //
    url = "http://papers.nips.cc/paper/2276-stochastic-neighbor-embedding", //
    bibkey = "DBLP:conf/nips/HintonR02")
public class SNE<O> extends AbstractProjectionAlgorithm<Relation<DoubleVector>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SNE.class);

  /**
   * Minimum value for qij entries (even when duplicate)
   */
  protected static final double MIN_QIJ = 1e-12;

  /**
   * Scale of the initial solution.
   */
  protected static final double INITIAL_SOLUTION_SCALE = 1e-4;

  /**
   * Minimum gain in learning rate.
   */
  protected static final double MIN_GAIN = 0.01;

  /**
   * Affinity matrix builder.
   */
  protected AffinityMatrixBuilder<? super O> affinity;

  /**
   * Number of distance computations performed in projected space.
   */
  protected long projectedDistances;

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
   * Iteration when to switch momentum.
   */
  protected int momentumSwitch = 250;

  /**
   * Number of iterations.
   */
  protected int iterations;

  /**
   * Random generator
   */
  protected RandomFactory random;

  /**
   * Constructor with default values.
   *
   * @param affinity Affinity matrix builder
   * @param dim Output dimensionality
   * @param random Random generator
   */
  public SNE(AffinityMatrixBuilder<? super O> affinity, int dim, RandomFactory random) {
    this(affinity, dim, 0.8, 200, 1000, random, true);
  }

  /**
   * Constructor.
   *
   * @param affinity Affinity matrix builder
   * @param dim Output dimensionality
   * @param finalMomentum Final momentum
   * @param learningRate Learning rate
   * @param iterations Number of iterations
   * @param random Random generator
   * @param keep Keep the original data (or remove it)
   */
  public SNE(AffinityMatrixBuilder<? super O> affinity, int dim, double finalMomentum, double learningRate, int iterations, RandomFactory random, boolean keep) {
    super(keep);
    this.affinity = affinity;
    this.dim = dim;
    this.iterations = iterations;
    this.learningRate = learningRate;
    this.initialMomentum = finalMomentum >= 0.6 ? 0.5 : (0.5 * finalMomentum);
    this.finalMomentum = finalMomentum;
    this.momentumSwitch = iterations / 4;
    this.random = random;
  }

  public Relation<DoubleVector> run(Relation<O> relation) {
    AffinityMatrix pij = affinity.computeAffinityMatrix(relation, 1.);

    // Create initial solution.
    final int size = pij.size();
    double[][] sol = randomInitialSolution(size, dim, random.getSingleThreadedRandom());
    projectedDistances = 0L;
    optimizeSNE(pij, sol);
    LOG.statistics(new LongStatistic(getClass().getName() + ".projected-distances", projectedDistances));

    // Remove the original (unprojected) data unless configured otherwise.
    removePreviousRelation(relation);

    // Transform into output data format.
    DBIDs ids = relation.getDBIDs();
    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(DBIDArrayIter it = pij.iterDBIDs(); it.valid(); it.advance()) {
      proj.put(it, DoubleVector.wrap(sol[it.getOffset()]));
    }

    return new MaterializedRelation<>("SNE", "SNE", otype, proj, ids);
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
   * Perform the actual tSNE optimization.
   * 
   * @param pij Initial affinity matrix
   * @param sol Solution output array (preinitialized)
   */
  protected void optimizeSNE(AffinityMatrix pij, double[][] sol) {
    final int size = pij.size();
    if(size * 3L * dim > 0x7FFF_FFFAL) {
      throw new AbortException("Memory exceeds Java array size limit.");
    }
    // Meta information on each point; joined for memory locality.
    // Gradient, Momentum, and learning rate
    // For performance, we use a flat memory layout!
    double[] meta = new double[size * 3 * dim];
    final int dim3 = dim * 3;
    for(int off = 2 * dim; off < meta.length; off += dim3) {
      Arrays.fill(meta, off, off + dim, 1.); // Initial learning rate
    }
    // Affinity matrix in projected space
    double[][] qij = new double[size][size];

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Iterative Optimization", iterations, LOG) : null;
    Duration timer = LOG.isStatistics() ? LOG.newDuration(this.getClass().getName() + ".runtime.optimization").begin() : null;
    // Optimize
    for(int it = 0; it < iterations; it++) {
      double qij_sum = computeQij(qij, sol);
      computeGradient(pij, qij, 1. / qij_sum, sol, meta);
      updateSolution(sol, meta, it);
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
  protected double computeQij(double[][] qij, double[][] solution) {
    double qij_sum = 0;
    for(int i = 1; i < qij.length; i++) {
      final double[] qij_i = qij[i], vi = solution[i];
      for(int j = 0; j < i; j++) {
        qij_sum += qij_i[j] = qij[j][i] = MathUtil.exp(-sqDist(vi, solution[j]));
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
  protected double sqDist(double[] v1, double[] v2) {
    assert (v1.length == v2.length) : "Lengths do not agree: " + v1.length + " " + v2.length;
    double sum = 0;
    for(int i = 0; i < v1.length; i++) {
      final double diff = v1[i] - v2[i];
      sum += diff * diff;
    }
    ++projectedDistances;
    return sum;
  }

  /**
   * Compute the gradients.
   * 
   * @param pij Desired affinity matrix
   * @param qij Projected affinity matrix
   * @param qij_isum Normalization factor
   * @param sol Current solution coordinates
   * @param meta Point metadata
   */
  protected void computeGradient(AffinityMatrix pij, double[][] qij, double qij_isum, double[][] sol, double[] meta) {
    final int dim3 = dim * 3;
    int size = pij.size();
    for(int i = 0, off = 0; i < size; i++, off += dim3) {
      final double[] sol_i = sol[i], qij_i = qij[i];
      Arrays.fill(meta, off, off + dim, 0.); // Clear gradient only
      for(int j = 0; j < size; j++) {
        if(i == j) {
          continue;
        }
        final double[] sol_j = sol[j];
        final double qij_ij = qij_i[j];
        // Qij after scaling!
        final double q = MathUtil.max(qij_ij * qij_isum, MIN_QIJ);
        double a = 4 * (pij.get(i, j) - q); // SNE gradient
        for(int k = 0; k < dim; k++) {
          meta[off + k] += a * (sol_i[k] - sol_j[k]);
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
  protected void updateSolution(double[][] sol, double[] meta, int it) {
    final double mom = (it < momentumSwitch && initialMomentum < finalMomentum) ? initialMomentum : finalMomentum;
    final int dim3 = dim * 3;
    for(int i = 0, off = 0; i < sol.length; i++, off += dim3) {
      final double[] sol_i = sol[i];
      for(int k = 0; k < dim; k++) {
        // Indexes in meta array
        final int gradk = off + k, movk = gradk + dim, gaink = movk + dim;
        // Adjust learning rate:
        meta[gaink] = MathUtil.max(((meta[gradk] > 0) != (meta[movk] > 0)) ? (meta[gaink] + 0.2) : (meta[gaink] * 0.8), MIN_GAIN);
        meta[movk] *= mom; // Dampening the previous momentum
        meta[movk] -= learningRate * meta[gradk] * meta[gaink]; // Learn
        sol_i[k] += meta[movk];
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(affinity.getInputTypeRestriction());
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
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Affinity matrix builder.
     */
    public static final OptionID AFFINITY_ID = new OptionID("tsne.affinity", "Affinity matrix builder.");

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
     * Number of iterations to execute.
     */
    public static final OptionID ITER_ID = new OptionID("tsne.iter", "Number of iterations to perform.");

    /**
     * Random generator seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("tsne.seed", "Random generator seed");

    /**
     * Affinity matrix builder.
     */
    protected AffinityMatrixBuilder<? super O> affinity;

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
    protected double finalMomentum;

    /**
     * Number of iterations.
     */
    protected int iterations;

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

      ObjectParameter<AffinityMatrixBuilder<? super O>> affinityP = new ObjectParameter<>(AFFINITY_ID, AffinityMatrixBuilder.class, getDefaultAffinity());
      if(config.grab(affinityP)) {
        affinity = affinityP.instantiateClass(config);
      }

      IntParameter dimP = new IntParameter(DIM_ID) //
          .setDefaultValue(2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimP)) {
        dim = dimP.intValue();
      }

      DoubleParameter momentumP = new DoubleParameter(MOMENTUM_ID)//
          .setDefaultValue(0.8) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(momentumP)) {
        finalMomentum = momentumP.doubleValue();
      }

      // Note that original tSNE defaulted to 100, Barnes-Hut variant to 200.
      DoubleParameter learningRateP = new DoubleParameter(LEARNING_RATE_ID)//
          .setDefaultValue(200.) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(learningRateP)) {
        learningRate = learningRateP.doubleValue();
      }

      IntParameter maxiterP = new IntParameter(ITER_ID)//
          .setDefaultValue(1000)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        iterations = maxiterP.intValue();
      }

      RandomParameter randP = new RandomParameter(RANDOM_ID);
      if(config.grab(randP)) {
        random = randP.getValue();
      }

      Flag keepF = new Flag(KEEP_ID);
      keep = config.grab(keepF) && keepF.isTrue();
    }

    /**
     * Get the default value for the affinity matrix builder.
     * 
     * @return Class
     */
    protected Class<?> getDefaultAffinity() {
      return PerplexityAffinityMatrixBuilder.class;
    }

    @Override
    protected SNE<O> makeInstance() {
      return new SNE<>(affinity, dim, finalMomentum, learningRate, iterations, random, keep);
    }
  }
}
