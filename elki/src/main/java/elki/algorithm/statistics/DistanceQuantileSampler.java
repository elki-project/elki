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
package elki.algorithm.statistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.result.CollectionResult;
import elki.result.Metadata;
import elki.utilities.datastructures.heap.DoubleMaxHeap;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Compute a quantile of a distance sample, useful for choosing parameters for
 * algorithms.
 * <p>
 * TODO: allow specifying multiple quantiles.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
public class DistanceQuantileSampler<O> implements Algorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(DistanceQuantileSampler.class);

  /**
   * Statistics prefix.
   */
  private static final String PREFIX = DistanceQuantileSampler.class.getName();

  /**
   * Distance function used.
   */
  private Distance<? super O> distance;

  /**
   * Quantile to compute.
   */
  private double quantile;

  /**
   * Sampling rate.
   */
  private double sampling;

  /**
   * Flag to ignore zero distances (recommended with many duplicates).
   */
  private boolean nozeros;

  /**
   * Random generator.
   */
  private RandomFactory rand;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param quantile Quantile to compute
   * @param sampling Sampling rate
   * @param nozeros Flag to ignore zero distances (recommended with many
   *        duplicates)
   * @param rand Random generator
   */
  public DistanceQuantileSampler(Distance<? super O> distance, double quantile, double sampling, boolean nozeros, RandomFactory rand) {
    super();
    this.distance = distance;
    this.quantile = quantile;
    this.sampling = sampling;
    this.nozeros = nozeros;
    this.rand = rand;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the distance quantile sampler.
   * 
   * @param relation Data relation
   * @return Distances sample
   */
  public CollectionResult<double[]> run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    int size = relation.size();
    long pairs = (size * (long) size) >> 1;

    final long ssize = sampling <= 1 ? (long) Math.ceil(sampling * pairs) : (long) sampling;
    if(ssize > Integer.MAX_VALUE) {
      throw new AbortException("Sampling size too large.");
    }
    final int qsize = quantile <= 0 ? 1 : (int) Math.ceil(quantile * ssize);

    DoubleMaxHeap heap = new DoubleMaxHeap(qsize);

    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    DBIDArrayIter i1 = ids.iter(), i2 = ids.iter();
    Random r = rand.getSingleThreadedRandom();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Sampling", (int) ssize, LOG) : null;
    for(long i = 0; i < ssize; i++) {
      int x = r.nextInt(size - 1) + 1, y = r.nextInt(x);
      double dist = dq.distance(i1.seek(x), i2.seek(y));
      // Skip NaN, and/or zeros.
      if(dist != dist || (nozeros && dist < Double.MIN_NORMAL)) {
        continue;
      }
      heap.add(dist, qsize);
      LOG.incrementProcessed(prog);
    }

    LOG.statistics(new DoubleStatistic(PREFIX + ".quantile", quantile));
    LOG.statistics(new LongStatistic(PREFIX + ".samplesize", ssize));
    LOG.statistics(new DoubleStatistic(PREFIX + ".distance", heap.peek()));
    LOG.ensureCompleted(prog);
    Collection<String> header = Arrays.asList(new String[] { "Distance" });
    Collection<double[]> data = Arrays.asList(new double[][] { new double[] { heap.peek() } });
    CollectionResult<double[]> result = new CollectionResult<>(data, header);
    Metadata.of(result).setLongName("Distances Sample");
    return result;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Quantile to compute.
     */
    public static final OptionID QUANTILE_ID = new OptionID("distsample.quantile", "Quantile to compute.");

    /**
     * Sampling rate.
     */
    public static final OptionID SAMPLING_ID = new OptionID("distsample.sample", "Number of distances to compute, either relative (values less than 1), or absolute.");

    /**
     * Flag to ignore zero distances (recommended with many duplicates).
     */
    public static final OptionID NOZEROS_ID = new OptionID("distsample.nozeros", "Ignore zero distances, beneficial for data sets with many duplicates.");

    /**
     * Random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("distsample.seed", "Random generator seed.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Quantile to compute.
     */
    protected double quantile;

    /**
     * Sampling rate.
     */
    protected double sampling;

    /**
     * Flag to ignore zero distances (recommended with many duplicates).
     */
    protected boolean nozeros;

    /**
     * Random generator.
     */
    protected RandomFactory rand;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(QUANTILE_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> quantile = x);
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> sampling = x);
      new Flag(NOZEROS_ID).grab(config, x -> nozeros = x);
      new RandomParameter(SEED_ID).grab(config, x -> rand = x);
    }

    @Override
    public DistanceQuantileSampler<O> make() {
      return new DistanceQuantileSampler<O>(distance, quantile, sampling, nozeros, rand);
    }
  }
}
