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
package elki.application.statistics;

import elki.application.AbstractDistanceBasedApplication;
import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.MeanVariance;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import elki.workflow.InputStep;

/**
 * Evaluate the range query selectivity.
 * <p>
 * TODO: Add sampling
 *
 * @author Erich Schubert
 * @since 0.7.0
 * @param <V> Vector type
 */
public class RangeQuerySelectivity<V extends NumberVector> extends AbstractDistanceBasedApplication<V> {
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
   * @param inputstep Input step
   * @param distance Distance function
   * @param radius Radius
   * @param sampling Sampling rate
   * @param random Random sampling generator
   */
  public RangeQuerySelectivity(InputStep inputstep, Distance<? super V> distance, double radius, double sampling, RandomFactory random) {
    super(inputstep, distance);
    this.radius = radius;
    this.sampling = sampling;
    this.random = random;
  }

  @Override
  public void run() {
    Relation<V> relation = inputstep.getDatabase().getRelation(distance.getInputTypeRestriction());
    RangeSearcher<DBIDRef> rangeQuery = new QueryBuilder<>(relation, distance).rangeByDBID(radius);
    DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);

    MeanVariance numres = new MeanVariance();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Performing range queries", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      numres.put(rangeQuery.getRange(iter, radius).size());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    final String prefix = this.getClass().getName();
    LOG.statistics(new DoubleStatistic(prefix + ".mean", numres.getMean()));
    LOG.statistics(new DoubleStatistic(prefix + ".std", numres.getSampleStddev()));
    LOG.statistics(new DoubleStatistic(prefix + ".norm.mean", numres.getMean() / relation.size()));
    LOG.statistics(new DoubleStatistic(prefix + ".norm.std", numres.getSampleStddev() / relation.size()));
    LOG.statistics(new LongStatistic(prefix + ".samplesize", ids.size()));
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
  public static class Par<V extends NumberVector> extends AbstractDistanceBasedApplication.Par<V> {
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
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(RADIUS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> radius = x);
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new RandomParameter(SEED_ID).grab(config, x -> random = x);
    }

    @Override
    public RangeQuerySelectivity<V> make() {
      return new RangeQuerySelectivity<>(inputstep, distance, radius, sampling, random);
    }
  }
}
