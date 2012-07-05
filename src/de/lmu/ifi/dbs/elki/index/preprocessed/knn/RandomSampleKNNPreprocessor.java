package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.GenericKNNHeap;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Class that computed the kNN only on a random sample.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class RandomSampleKNNPreprocessor<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D, KNNResult<D>> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomSampleKNNPreprocessor.class);

  /**
   * Relative share of objects to get
   */
  private final double share;

  /**
   * Random seed
   */
  private final Long seed;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param distanceFunction distance function
   * @param k k
   * @param share Relative share
   * @param seed Random seed (may be null)
   */
  public RandomSampleKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k, double share, Long seed) {
    super(relation, distanceFunction, k);
    this.share = share;
    this.seed = seed;
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNResult.class);
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing random-sample k nearest neighbors (k=" + k + ")", relation.size(), getLogger()) : null;

    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int samplesize = (int) (ids.size() * share);
    final long iseed = (seed != null) ? seed : (new Random()).nextLong();

    int i = 0;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      GenericKNNHeap<D> kNN = new GenericKNNHeap<D>(k);

      long rseed = i * 0x7FFFFFFFFFFFFFE7L + iseed;
      DBIDs rsamp = DBIDUtil.randomSample(ids, samplesize, rseed);
      for (DBIDIter iter2 = rsamp.iter(); iter2.valid(); iter2.advance()) {
        D dist = distanceQuery.distance(iter, iter2);
        kNN.add(dist, iter2);
      }

      storage.put(iter, kNN.toKNNList());
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "random sample kNN";
  }

  @Override
  public String getShortName() {
    return "random-sample-knn";
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractMaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D, KNNResult<D>> {
    /**
     * Relative share of objects to get
     */
    private final double share;

    /**
     * Random seed
     */
    private final Long seed;

    /**
     * Constructor.
     * 
     * @param k K
     * @param distanceFunction distance function
     * @param share Sample size (relative)
     * @param seed Random seed
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction, double share, Long seed) {
      super(k, distanceFunction);
      this.share = share;
      this.seed = seed;
    }

    @Override
    public RandomSampleKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      return new RandomSampleKNNPreprocessor<O, D>(relation, distanceFunction, k, share, seed);
    }

    /**
     * Parameterization class
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     * 
     * @param <O> Object type
     * @param <D> Distance type
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      /**
       * Parameter to specify how many objects to consider for computing the
       * kNN.
       * 
       * <p>
       * Key: {@code -randomknn.share}
       * </p>
       */
      public static final OptionID SHARE_ID = OptionID.getOrCreateOptionID("randomknn.share", "The relative amount of objects to consider for kNN computations.");

      /**
       * Random number generator seed.
       * 
       * <p>
       * Key: {@code -randomknn.seed}
       * </p>
       */
      public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("randomknn.seed", "The random number seed.");

      /**
       * Relative share of objects to get
       */
      private double share = 0.0;

      /**
       * Random seed
       */
      private Long seed = null;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DoubleParameter shareP = new DoubleParameter(SHARE_ID, new IntervalConstraint(0.0, IntervalBoundary.OPEN, 1.0, IntervalBoundary.OPEN));
        if(config.grab(shareP)) {
          share = shareP.getValue();
        }
        LongParameter seedP = new LongParameter(SEED_ID, true);
        if(config.grab(seedP)) {
          seed = seedP.getValue();
        }
      }

      @Override
      protected RandomSampleKNNPreprocessor.Factory<O, D> makeInstance() {
        return new RandomSampleKNNPreprocessor.Factory<O, D>(k, distanceFunction, share, seed);
      }
    }
  }
}