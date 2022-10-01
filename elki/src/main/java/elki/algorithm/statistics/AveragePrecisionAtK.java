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

import java.util.ArrayList;
import java.util.Collection;

import elki.Algorithm;
import elki.data.LabelList;
import elki.data.type.AlternativeTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVarianceMinMax;
import elki.result.CollectionResult;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.random.RandomFactory;

/**
 * Evaluate a distance functions performance by computing the average precision
 * at k, when ranking the objects by distance.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type
 */
public class AveragePrecisionAtK<O> implements Algorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AveragePrecisionAtK.class);

  /**
   * Distance function used.
   */
  private Distance<? super O> distance;

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
   * @param distance Distance function
   * @param k K parameter
   * @param sampling Sampling rate
   * @param random Random sampling generator
   * @param includeSelf Include query object in evaluation
   */
  public AveragePrecisionAtK(Distance<? super O> distance, int k, double sampling, RandomFactory random, boolean includeSelf) {
    super();
    this.distance = distance;
    this.k = k;
    this.sampling = sampling;
    this.random = random;
    this.includeSelf = includeSelf;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction(), //
        new AlternativeTypeInformation(TypeUtil.CLASSLABEL, TypeUtil.LABELLIST));
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation for distance computations
   * @param lrelation Relation for class label comparison
   * @return Vectors containing mean and standard deviation.
   */
  public CollectionResult<double[]> run(Relation<O> relation, Relation<?> lrelation) {
    final int qk = k + (includeSelf ? 0 : 1);
    KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).kNNByDBID(qk);
    final DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);

    MeanVarianceMinMax[] mvs = MeanVarianceMinMax.newArray(k);
    FiniteProgress objloop = LOG.isVerbose() ? new FiniteProgress("Computing nearest neighbors", ids.size(), LOG) : null;
    // sort neighbors
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      Object label = lrelation.get(iter);
      int positive = 0, i = 0;
      for(DBIDIter ri = knnQuery.getKNN(iter, qk).iter(); i < k && ri.valid(); ri.advance()) {
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
    CollectionResult<double[]> result = new CollectionResult<>(res);
    Metadata.of(result).setLongName("Average Precision");
    return result;
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
    // Fallback to equality, e.g., on class labels
    return ref.equals(test);
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
     * The distance function to use.
     */
    protected Distance<? super O> distance;

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
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new RandomParameter(SEED_ID).grab(config, x -> seed = x);
      new Flag(INCLUDESELF_ID).grab(config, x -> includeSelf = x);
    }

    @Override
    public AveragePrecisionAtK<O> make() {
      return new AveragePrecisionAtK<>(distance, k, sampling, seed, includeSelf);
    }
  }
}
