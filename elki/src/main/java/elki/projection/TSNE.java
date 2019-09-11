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
package elki.projection;

import java.util.Arrays;
import java.util.Random;

import elki.data.DoubleVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * t-Stochastic Neighbor Embedding is a projection technique designed for
 * visualization that tries to preserve the nearest neighbor structure.
 * <p>
 * Reference:
 * <p>
 * L. J. P. van der Maaten, G. E. Hinton<br>
 * Visualizing High-Dimensional Data Using t-SNE<br>
 * Journal of Machine Learning Research 9
 *
 * @author Erich Schubert
 * @author Dominik Acker
 * @since 0.7.5
 *
 * @composed - - - AffinityMatrixBuilder
 *
 * @param <O> Object type
 */
@Title("t-SNE")
@Reference(authors = "L. J. P. van der Maaten, G. E. Hinton", //
    title = "Visualizing High-Dimensional Data Using t-SNE", //
    booktitle = "Journal of Machine Learning Research 9", //
    url = "http://www.jmlr.org/papers/v9/vandermaaten08a.html", //
    bibkey = "journals/jmlr/MaatenH08")
@Alias({ "t-SNE", "tSNE" })
public class TSNE<O> extends AbstractProjectionAlgorithm<Relation<DoubleVector>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(TSNE.class);

  /**
   * Minimum value for qij entries (even when duplicate)
   */
  protected static final double MIN_QIJ = 1e-12;

  /**
   * Early exaggeration factor.
   * <p>
   * Barnes-Hut tSNE implementation used 12.
   */
  protected static final double EARLY_EXAGGERATION = 4.;

  /**
   * Number of iterations to apply early exaggeration.
   */
  protected static final int EARLY_EXAGGERATION_ITERATIONS = 50;

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
  public TSNE(AffinityMatrixBuilder<? super O> affinity, int dim, RandomFactory random) {
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
  public TSNE(AffinityMatrixBuilder<? super O> affinity, int dim, double finalMomentum, double learningRate, int iterations, RandomFactory random, boolean keep) {
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
    AffinityMatrix pij = affinity.computeAffinityMatrix(relation, EARLY_EXAGGERATION);

    // Create initial solution.
    final int size = pij.size();
    double[][] sol = randomInitialSolution(size, dim, random.getSingleThreadedRandom());
    projectedDistances = 0L;
    optimizetSNE(pij, sol);
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

    return new MaterializedRelation<>("t-SNE", otype, ids, proj);
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
  protected void optimizetSNE(AffinityMatrix pij, double[][] sol) {
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
      // Undo early exaggeration
      if(it == EARLY_EXAGGERATION_ITERATIONS) {
        pij.scale(1. / EARLY_EXAGGERATION);
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
  protected double computeQij(double[][] qij, double[][] solution) {
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
        double a = (pij.get(i, j) - q) * qij_ij;
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
  public static class Par<O> implements Parameterizer {
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
    public void configure(Parameterization config) {

      new ObjectParameter<AffinityMatrixBuilder<? super O>>(AFFINITY_ID, AffinityMatrixBuilder.class, getDefaultAffinity()) //
          .grab(config, x -> affinity = x);
      new IntParameter(DIM_ID) //
          .setDefaultValue(2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> dim = x);
      new DoubleParameter(MOMENTUM_ID)//
          .setDefaultValue(0.8) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> finalMomentum = x);
      // Note that original tSNE defaulted to 100, Barnes-Hut variant to 200.
      new DoubleParameter(LEARNING_RATE_ID)//
          .setDefaultValue(200.) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> learningRate = x);
      new IntParameter(ITER_ID)//
          .setDefaultValue(1000)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> iterations = x);
      new RandomParameter(RANDOM_ID).grab(config, x -> random = x);
      new Flag(KEEP_ID).grab(config, x -> keep = x);
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
    public TSNE<O> make() {
      return new TSNE<>(affinity, dim, finalMomentum, learningRate, iterations, random, keep);
    }
  }
}
