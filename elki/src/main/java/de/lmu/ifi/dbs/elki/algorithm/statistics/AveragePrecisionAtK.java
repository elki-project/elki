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

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.AlternativeTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Evaluate a distance functions performance by computing the average precision
 * at k, when ranking the objects by distance.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type
 */
public class AveragePrecisionAtK<O> extends AbstractDistanceBasedAlgorithm<O, CollectionResult<DoubleVector>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AveragePrecisionAtK.class);

  /**
   * The parameter k - the number of neighbors to retrieve.
   */
  private int k;

  /**
   * Relative number of object to use in sampling.
   */
  private double sampling = 1.0;

  /**
   * Random sampling seed.
   */
  private RandomFactory random = null;

  /**
   * Include query object in evaluation.
   */
  private boolean includeSelf;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k K parameter
   * @param sampling Sampling rate
   * @param random Random sampling generator
   * @param includeSelf Include query object in evaluation
   */
  public AveragePrecisionAtK(DistanceFunction<? super O> distanceFunction, int k, double sampling, RandomFactory random, boolean includeSelf) {
    super(distanceFunction);
    this.k = k;
    this.sampling = sampling;
    this.random = random;
    this.includeSelf = includeSelf;
  }

  /**
   * Run the algorithm
   *
   * @param database Database to run on (for kNN queries)
   * @param relation Relation for distance computations
   * @param lrelation Relation for class label comparison
   * @return Vectors containing mean and standard deviation.
   */
  public CollectionResult<double[]> run(Database database, Relation<O> relation, Relation<?> lrelation) {
    final DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final int qk = k + (includeSelf ? 0 : 1);
    final KNNQuery<O> knnQuery = database.getKNNQuery(distQuery, qk);

    MeanVarianceMinMax[] mvs = MeanVarianceMinMax.newArray(k);

    final DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);

    FiniteProgress objloop = LOG.isVerbose() ? new FiniteProgress("Computing nearest neighbors", ids.size(), LOG) : null;
    // sort neighbors
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNList knn = knnQuery.getKNNForDBID(iter, qk);
      Object label = lrelation.get(iter);

      int positive = 0, i = 0;
      for(DBIDIter ri = knn.iter(); i < k && ri.valid(); ri.advance()) {
        if(!includeSelf && DBIDUtil.equal(iter, ri)) {
          // Do not increment i.
          continue;
        }
        positive += match(label, lrelation.get(ri)) ? 1 : 0;
        final double precision = positive / (double) (i + 1);
        mvs[i].put(precision);
        i++;
      }
      LOG.incrementProcessed(objloop);
    }
    LOG.ensureCompleted(objloop);

    // Transform Histogram into a Double Vector array.
    Collection<double[]> res = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      final MeanVarianceMinMax mv = mvs[i];
      final double std = mv.getCount() > 1. ? mv.getSampleStddev() : 0.;
      res.add(new double[] { i + 1, mv.getMean(), std, mv.getMin(), mv.getMax(), mv.getCount() });
    }
    return new CollectionResult<>("Average Precision", "average-precision", res);
  }

  /**
   * Test whether two relation agree.
   *
   * @param ref Reference object
   * @param test Test object
   * @return {@code true} if the objects match
   */
  protected static boolean match(Object ref, Object test) {
    if(ref == null) {
      return false;
    }
    // Cheap and fast, may hold for class labels!
    if(ref == test) {
      return true;
    }
    if(ref instanceof LabelList && test instanceof LabelList) {
      final LabelList lref = (LabelList) ref;
      final LabelList ltest = (LabelList) test;
      final int s1 = lref.size(), s2 = ltest.size();
      if(s1 == 0 || s2 == 0) {
        return false;
      }
      for(int i = 0; i < s1; i++) {
        String l1 = lref.get(i);
        if(l1 == null) {
          continue;
        }
        for(int j = 0; j < s2; j++) {
          if(l1.equals(ltest.get(j))) {
            return true;
          }
        }
      }
    }
    // Fallback to equality, e.g. on class labels
    return ref.equals(test);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation cls = new AlternativeTypeInformation(TypeUtil.CLASSLABEL, TypeUtil.LABELLIST);
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), cls);
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter k to compute the average precision at.
     */
    private static final OptionID K_ID = new OptionID("avep.k", "K to compute the average precision at.");

    /**
     * Parameter to enable sampling.
     */
    public static final OptionID SAMPLING_ID = new OptionID("avep.sampling", "Relative amount of object to sample.");

    /**
     * Parameter to control the sampling random seed.
     */
    public static final OptionID SEED_ID = new OptionID("avep.sampling-seed", "Random seed for deterministic sampling.");

    /**
     * Parameter to include the query object.
     */
    public static final OptionID INCLUDESELF_ID = new OptionID("avep.includeself", "Include the query object in the evaluation.");

    /**
     * Neighborhood size.
     */
    protected int k = 20;

    /**
     * Relative amount of data to sample.
     */
    protected double sampling = 1.0;

    /**
     * Random sampling seed.
     */
    protected RandomFactory seed = null;

    /**
     * Include query object in evaluation.
     */
    protected boolean includeSelf;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
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
        seed = rndP.getValue();
      }
      final Flag includeP = new Flag(INCLUDESELF_ID);
      if(config.grab(includeP)) {
        includeSelf = includeP.isTrue();
      }
    }

    @Override
    protected AveragePrecisionAtK<O> makeInstance() {
      return new AveragePrecisionAtK<>(distanceFunction, k, sampling, seed, includeSelf);
    }
  }
}
