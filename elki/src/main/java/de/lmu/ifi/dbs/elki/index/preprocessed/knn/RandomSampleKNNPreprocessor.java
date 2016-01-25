package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Class that computed the kNN only on a random sample.
 *
 * Used in:
 * <p>
 * Subsampling for Efficient and Effective Unsupervised Outlier Detection
 * Ensembles<br />
 * A. Zimek and M. Gaudet and R. J. G. B. Campello and J. Sander<br />
 * In: Proc. 19th ACM SIGKDD International Conference on Knowledge Discovery and
 * Data Mining, KDD '13
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type
 */
@Reference(title = "Subsampling for Efficient and Effective Unsupervised Outlier Detection Ensembles", authors = "A. Zimek and M. Gaudet and R. J. G. B. Campello and J. Sander", booktitle = "Proc. 19th ACM SIGKDD International Conference on Knowledge Discovery and Data Mining, KDD '13")
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
  public RandomSampleKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k, double share, RandomFactory rnd) {
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
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractMaterializeKNNPreprocessor oneway - - «create»
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
    public Factory(int k, DistanceFunction<? super O> distanceFunction, double share, RandomFactory rnd) {
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
     * @apiviz.exclude
     *
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O> {
      /**
       * Parameter to specify how many objects to consider for computing the
       * kNN.
       *
       * <p>
       * Key: {@code -randomknn.share}
       * </p>
       */
      public static final OptionID SHARE_ID = new OptionID("randomknn.share", "The relative amount of objects to consider for kNN computations.");

      /**
       * Random number generator seed.
       *
       * <p>
       * Key: {@code -randomknn.seed}
       * </p>
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
        DoubleParameter shareP = new DoubleParameter(SHARE_ID);
        shareP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
        shareP.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
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
