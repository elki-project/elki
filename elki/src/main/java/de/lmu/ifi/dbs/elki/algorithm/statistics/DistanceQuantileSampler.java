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
package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Compute a quantile of a distance sample, useful for choosing parameters for
 * algorithms.
 *
 * TODO: allow specifying multiple quantiles.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
public class DistanceQuantileSampler<O> extends AbstractDistanceBasedAlgorithm<O, CollectionResult<double[]>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(DistanceQuantileSampler.class);

  /**
   * Statistics prefix.
   */
  private static final String PREFIX = DistanceQuantileSampler.class.getName();

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
   * @param distanceFunction Distance function
   * @param quantile Quantile to compute
   * @param sampling Sampling rate
   * @param nozeros Flag to ignore zero distances (recommended with many
   *        duplicates)
   * @param rand Random generator
   */
  public DistanceQuantileSampler(DistanceFunction<? super O> distanceFunction, double quantile, double sampling, boolean nozeros, RandomFactory rand) {
    super(distanceFunction);
    this.quantile = quantile;
    this.sampling = sampling;
    this.nozeros = nozeros;
    this.rand = rand;
  }

  /**
   * Run the distance quantile sampler.
   * 
   * @param database
   * @param rel
   * @return Distances sample
   */
  public CollectionResult<double[]> run(Database database, Relation<O> rel) {
    DistanceQuery<O> dq = rel.getDistanceQuery(getDistanceFunction());
    int size = rel.size();
    long pairs = (size * (long) size) >> 1;

    final long ssize = sampling <= 1 ? (long) Math.ceil(sampling * pairs) : (long) sampling;
    if(ssize > Integer.MAX_VALUE) {
      throw new AbortException("Sampling size too large.");
    }
    final int qsize = quantile <= 0 ? 1 : (int) Math.ceil(quantile * ssize);

    DoubleMaxHeap heap = new DoubleMaxHeap(qsize);

    ArrayDBIDs ids = DBIDUtil.ensureArray(rel.getDBIDs());
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
    return new CollectionResult<double[]>("Distances sample", "distance-sample", data, header);
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
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter quantileP = new DoubleParameter(QUANTILE_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(quantileP)) {
        quantile = quantileP.doubleValue();
      }

      DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }

      Flag nozerosF = new Flag(NOZEROS_ID);
      if(config.grab(nozerosF)) {
        nozeros = nozerosF.isTrue();
      }

      RandomParameter randP = new RandomParameter(SEED_ID);
      if(config.grab(randP)) {
        rand = randP.getValue();
      }
    }

    @Override
    protected DistanceQuantileSampler<O> makeInstance() {
      return new DistanceQuantileSampler<O>(distanceFunction, quantile, sampling, nozeros, rand);
    }
  }
}
