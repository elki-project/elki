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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Evaluate the range query selectivity.
 *
 * TODO: Add sampling
 *
 * @author Erich Schubert
 * @since 0.7.0
 * @param <V> Vector type
 */
public class RangeQuerySelectivity<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<V, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RangeQuerySelectivity.class);

  /**
   * Query radius
   */
  protected double radius;

  /**
   * Relative number of object to use in sampling.
   */
  protected double sampling = 1.0;

  /**
   * Random sampling seed.
   */
  protected RandomFactory random = null;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param radius Radius
   * @param sampling Sampling rate
   * @param random Random sampling generator
   */
  public RangeQuerySelectivity(DistanceFunction<? super V> distanceFunction, double radius, double sampling, RandomFactory random) {
    super(distanceFunction);
    this.radius = radius;
    this.sampling = sampling;
    this.random = random;
  }

  public Result run(Database database, Relation<V> relation) {
    DistanceQuery<V> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<V> rangeQuery = database.getRangeQuery(distQuery, radius);

    MeanVariance numres = new MeanVariance();

    final DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Performing range queries", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      numres.put(rangeQuery.getRangeForDBID(iter, radius).size());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    final String prefix = this.getClass().getName();
    LOG.statistics(new DoubleStatistic(prefix + ".mean", numres.getMean()));
    LOG.statistics(new DoubleStatistic(prefix + ".std", numres.getSampleStddev()));
    LOG.statistics(new DoubleStatistic(prefix + ".norm.mean", numres.getMean() / relation.size()));
    LOG.statistics(new DoubleStatistic(prefix + ".norm.std", numres.getSampleStddev() / relation.size()));
    LOG.statistics(new LongStatistic(prefix + ".samplesize", ids.size()));
    return null;
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
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * Parameter to specify the query radius.
     */
    public static final OptionID RADIUS_ID = new OptionID("selectivity.radius", "Radius to use for selectivity estimation.");

    /**
     * Parameter to enable sampling.
     */
    public static final OptionID SAMPLING_ID = new OptionID("selectivity.sampling", "Relative amount of object to sample.");

    /**
     * Parameter to control the sampling random seed.
     */
    public static final OptionID SEED_ID = new OptionID("selectivity.sampling-seed", "Random seed for deterministic sampling.");

    /**
     * Query radius
     */
    protected double radius;

    /**
     * Relative number of object to use in sampling.
     */
    protected double sampling = 1.0;

    /**
     * Random sampling seed.
     */
    protected RandomFactory random = RandomFactory.DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter param = new DoubleParameter(RADIUS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(param)) {
        radius = param.doubleValue();
      }
      final DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.getValue();
      }
      final RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        random = rndP.getValue();
      }
    }

    @Override
    protected RangeQuerySelectivity<V> makeInstance() {
      return new RangeQuerySelectivity<>(distanceFunction, radius, sampling, random);
    }
  }
}
