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
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.math.statistics.intrinsicdimensionality.GEDEstimator;
import elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;
import elki.workflow.InputStep;

/**
 * Estimate global average intrinsic dimensionality of a data set.
 * <p>
 * The output will be logged at the statistics level.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Data type
 */
public class EstimateIntrinsicDimensionality<O> extends AbstractDistanceBasedApplication<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(EstimateIntrinsicDimensionality.class);

  /**
   * Number of neighbors to use.
   */
  protected double krate;

  /**
   * Number of samples to draw.
   */
  protected double samples;

  /**
   * Estimation method.
   */
  protected IntrinsicDimensionalityEstimator<? super O> estimator;

  /**
   * Constructor.
   *
   * @param inputstep Data input step
   * @param distance Distance function
   * @param estimator Estimator
   * @param krate kNN rate
   * @param samples Sample size
   */
  public EstimateIntrinsicDimensionality(InputStep inputstep, Distance<? super O> distance, IntrinsicDimensionalityEstimator<? super O> estimator, double krate, double samples) {
    super(inputstep, distance);
    this.estimator = estimator;
    this.krate = krate;
    this.samples = samples;
  }

  @Override
  public void run() {
    Database database = inputstep.getDatabase();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DBIDs allids = relation.getDBIDs();
    // Number of samples to draw.
    int ssize = (int) ((samples > 1.) ? samples : Math.ceil(samples * allids.size()));
    // Number of neighbors to fetch (+ query point)
    int kk = 1 + (int) ((krate > 1.) ? krate : Math.ceil(krate * allids.size()));

    DBIDs sampleids = DBIDUtil.randomSample(allids, ssize, RandomFactory.DEFAULT);
    QueryBuilder<O> qb = new QueryBuilder<>(relation, distance);
    DistanceQuery<O> distq = qb.distanceQuery();
    KNNSearcher<DBIDRef> knnq = qb.kNNByDBID(kk);

    double[] idim = new double[ssize];
    int samples = 0;
    for(DBIDIter iter = sampleids.iter(); iter.valid(); iter.advance()) {
      idim[samples++] = estimator.estimate(knnq, distq, iter, kk);
    }
    double id = (samples > 1) ? QuickSelect.median(idim, 0, samples) : -1;
    LOG.statistics(new DoubleStatistic(EstimateIntrinsicDimensionality.class.getName() + ".intrinsic-dimensionality", id));
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
  public static class Par<O> extends AbstractDistanceBasedApplication.Par<O> {
    /**
     * Estimation method
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("idist.estimator", "Estimation method for intrinsic dimensionality.");

    /**
     * Number of kNN to use for each object.
     */
    public static final OptionID KRATE_ID = new OptionID("idist.k", "Number of kNN (absolute or relative)");

    /**
     * Number of samples to draw from the data set.
     */
    public static final OptionID SAMPLES_ID = new OptionID("idist.sampling", "Sample size (absolute or relative)");

    /**
     * Estimation method.
     */
    protected IntrinsicDimensionalityEstimator<? super O> estimator;

    /**
     * Number of neighbors to use.
     */
    protected double krate;

    /**
     * Number of samples to draw.
     */
    protected double samples;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<IntrinsicDimensionalityEstimator<? super O>>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, GEDEstimator.class) //
          .grab(config, x -> estimator = x);
      new DoubleParameter(KRATE_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> krate = x);
      new DoubleParameter(SAMPLES_ID, .1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> samples = x);
    }

    @Override
    public EstimateIntrinsicDimensionality<O> make() {
      return new EstimateIntrinsicDimensionality<>(inputstep, distance, estimator, krate, samples);
    }
  }
}
