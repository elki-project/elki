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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

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
  private static final Logging LOG = Logging.getLogger(RandomSampleKNNPreprocessor.class);

  /**
   * Relative share of objects to get
   */
  private final double share;

  /**
   * Random generator
   */
  private final RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param distanceFunction distance function
   * @param k k
   * @param share Relative share
   * @param rnd Random generator
   */
  public RandomSampleKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k, double share, RandomFactory rnd) {
    super(relation, distanceFunction, k);
    this.share = share;
    this.rnd = rnd;
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNResult.class);
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing random-sample k nearest neighbors (k=" + k + ")", relation.size(), getLogger()) : null;

    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int samplesize = (int) (ids.size() * share);
    final long iseed = rnd.getRandom().nextLong();

    int i = 0;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNHeap<D> kNN = KNNUtil.newHeap(distanceFunction, k);

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
    return LOG;
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
     * Random generator
     */
    private final RandomFactory rnd;

    /**
     * Constructor.
     * 
     * @param k K
     * @param distanceFunction distance function
     * @param share Sample size (relative)
     * @param rnd Random generator
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction, double share, RandomFactory rnd) {
      super(k, distanceFunction);
      this.share = share;
      this.rnd = rnd;
    }

    @Override
    public RandomSampleKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      return new RandomSampleKNNPreprocessor<O, D>(relation, distanceFunction, k, share, rnd);
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
       * Random generator
       */
      private RandomFactory rnd;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DoubleParameter shareP = new DoubleParameter(SHARE_ID);
        shareP.addConstraint(new GreaterConstraint(0.0));
        shareP.addConstraint(new LessConstraint(1.0));
        if(config.grab(shareP)) {
          share = shareP.getValue();
        }
        RandomParameter rndP = new RandomParameter(SEED_ID);
        if(config.grab(rndP)) {
          rnd = rndP.getValue();
        }
      }

      @Override
      protected RandomSampleKNNPreprocessor.Factory<O, D> makeInstance() {
        return new RandomSampleKNNPreprocessor.Factory<O, D>(k, distanceFunction, share, rnd);
      }
    }
  }
}