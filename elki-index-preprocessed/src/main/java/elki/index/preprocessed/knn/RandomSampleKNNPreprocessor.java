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
package elki.index.preprocessed.knn;

import java.util.Random;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNHeap;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Class that computed the kNN only on a random sample.
 * <p>
 * Used in:
 * <p>
 * Arthur Zimek, Matthew Gaudet, Ricardo J. G. B. Campello, Jörg Sander<br>
 * Subsampling for Efficient and Effective Unsupervised Outlier Detection
 * Ensembles<br>
 * Proc. 19th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining KDD'13
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type
 */
@Reference(authors = "Arthur Zimek, Matthew Gaudet, Ricardo J. G. B. Campello, Jörg Sander", //
    title = "Subsampling for Efficient and Effective Unsupervised Outlier Detection Ensembles", //
    booktitle = "Proc. 19th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining, KDD '13", //
    url = "https://doi.org/10.1145/2487575.2487676", //
    bibkey = "DBLP:conf/kdd/ZimekGCS13")
public class RandomSampleKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O> {
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
  public RandomSampleKNNPreprocessor(Relation<O> relation, Distance<? super O> distanceFunction, int k, double share, RandomFactory rnd) {
    super(relation, distanceFunction, k);
    this.share = share;
    this.rnd = rnd;
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction);
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing random-sample k nearest neighbors (k=" + k + ")", relation.size(), getLogger()) : null;

    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int samplesize = (int) (ids.size() * share);

    Random random = rnd.getSingleThreadedRandom();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNHeap kNN = DBIDUtil.newHeap(k);
      DBIDs rsamp = DBIDUtil.randomSample(ids, samplesize, random);
      for(DBIDIter iter2 = rsamp.iter(); iter2.valid(); iter2.advance()) {
        double dist = distanceQuery.distance(iter, iter2);
        kNN.insert(dist, iter2);
      }

      storage.put(iter, kNN.toKNNList());
      getLogger().incrementProcessed(progress);
    }

    getLogger().ensureCompleted(progress);
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

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * The parameterizable factory.
   *
   * @author Erich Schubert
   *
   * @opt nodefillcolor LemonChiffon
   * @stereotype factory
   * @navassoc - create - AbstractMaterializeKNNPreprocessor
   *
   * @param <O> The object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
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
    public Factory(int k, Distance<? super O> distanceFunction, double share, RandomFactory rnd) {
      super(k, distanceFunction);
      this.share = share;
      this.rnd = rnd;
    }

    @Override
    public RandomSampleKNNPreprocessor<O> instantiate(Relation<O> relation) {
      return new RandomSampleKNNPreprocessor<>(relation, distanceFunction, k, share, rnd);
    }

    /**
     * Parameterization class
     *
     * @author Erich Schubert
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O> {
      /**
       * Share of objects to consider for computing the kNN.
       */
      public static final OptionID SHARE_ID = new OptionID("randomknn.share", "The relative amount of objects to consider for kNN computations.");

      /**
       * Random number generator seed.
       */
      public static final OptionID SEED_ID = new OptionID("randomknn.seed", "The random number seed.");

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
        DoubleParameter shareP = new DoubleParameter(SHARE_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
        if(config.grab(shareP)) {
          share = shareP.getValue();
        }
        RandomParameter rndP = new RandomParameter(SEED_ID);
        if(config.grab(rndP)) {
          rnd = rndP.getValue();
        }
      }

      @Override
      protected RandomSampleKNNPreprocessor.Factory<O> makeInstance() {
        return new RandomSampleKNNPreprocessor.Factory<>(k, distanceFunction, share, rnd);
      }
    }
  }
}
