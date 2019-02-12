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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithmUtil;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Clustering Large Applications (CLARA) with the {@link KMedoidsFastPAM}
 * improvements, to increase scalability in the number of clusters. This variant
 * will also default to twice the sample size, to improve quality.
 * <p>
 * TODO: use a triangular distance matrix, rather than a hash-map based cache,
 * for a bit better performance and less memory.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Peter J. Rousseeuw<br>
 * Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS
 * Algorithms<br>
 * preprint, to appear
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - - - de.lmu.ifi.dbs.elki.data.model.MedoidModel
 *
 * @param <V> Data type
 */
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "preprint, to appear", //
    url = "https://arxiv.org/abs/1810.05691", //
    bibkey = "DBLP:journals/corr/abs-1810-05691")
public class FastCLARA<V> extends KMedoidsFastPAM<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastCLARA.class);

  /**
   * Sampling rate. If less than 1, it is considered to be a relative value.
   */
  double sampling;

  /**
   * Number of samples to draw (i.e. iterations).
   */
  int numsamples;

  /**
   * Keep the previous medoids in the sample (see page 145).
   */
  boolean keepmed;

  /**
   * Random factory for initialization.
   */
  RandomFactory random;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param k Number of clusters to produce
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization function
   * @param numsamples Number of samples (sampling iterations)
   * @param sampling Sampling rate (absolute or relative)
   * @param keepmed Keep the previous medoids in the next sample
   * @param random Random generator
   */
  public FastCLARA(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, double fasttol, int numsamples, double sampling, boolean keepmed, RandomFactory random) {
    super(distanceFunction, k, maxiter, initializer, fasttol);
    this.numsamples = numsamples;
    this.sampling = sampling;
    this.random = random;
    this.keepmed = keepmed;
  }

  @Override
  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("CLARA Clustering", "clara-clustering");
    }
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction());
    int samplesize = Math.min(ids.size(), (int) (sampling <= 1 ? sampling * ids.size() : sampling));
    if(samplesize < 3 * k) {
      LOG.warning("The sampling size is set to a very small value, it should be much larger than k.");
    }

    CLARA.CachedDistanceQuery<V> cachedQ = new CLARA.CachedDistanceQuery<V>(distQ, (samplesize * (samplesize - 1)) >> 1);

    double best = Double.POSITIVE_INFINITY;
    ArrayModifiableDBIDs bestmedoids = null;
    WritableIntegerDataStore bestclusters = null;

    Random rnd = random.getSingleThreadedRandom();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Processing random samples", numsamples, LOG) : null;
    for(int j = 0; j < numsamples; j++) {
      DBIDs rids = CLARA.randomSample(ids, samplesize, rnd, keepmed ? bestmedoids : null);
      cachedQ.clear(); // TODO: an actual matrix would be better.

      // Choose initial medoids
      ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, rids, cachedQ));
      // Setup cluster assignment store
      WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      double score = new /* PAM */Instance(cachedQ, rids, assignment, fasttol).run(medoids, maxiter) //
          + CLARA.assignRemainingToNearestCluster(medoids, ids, rids, assignment, distQ);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(getClass().getName() + ".sample-" + j + ".cost", score));
      }
      if(score < best) {
        best = score;
        bestmedoids = medoids;
        bestclusters = assignment;
      }
      if(cachedQ.hasUncachedQueries()) {
        LOG.warning("Some distance queries were not cached; maybe the initialization is not optimized for k-medoids.");
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".cost", best));
    }
    if (bestmedoids == null) {
      throw new IllegalStateException("numsamples must be larger than 0.");
    }

    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, bestclusters, k);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("CLARA Clustering", "clara-clustering");
    for(DBIDArrayIter it = bestmedoids.iter(); it.valid(); it.advance()) {
      MedoidModel model = new MedoidModel(DBIDUtil.deref(it));
      result.addToplevelCluster(new Cluster<>(clusters[it.getOffset()], model));
    }
    return result;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends KMedoidsFastPAM.Parameterizer<V> {
    /**
     * The number of samples to run.
     */
    public static final OptionID NUMSAMPLES_ID = CLARA.Parameterizer.NUMSAMPLES_ID;

    /**
     * The sample size.
     */
    public static final OptionID SAMPLESIZE_ID = CLARA.Parameterizer.SAMPLESIZE_ID;

    /**
     * Draw independent samples.
     */
    public static final OptionID NOKEEPMED_ID = CLARA.Parameterizer.NOKEEPMED_ID;

    /**
     * Random generator.
     */
    public static final OptionID RANDOM_ID = CLARA.Parameterizer.RANDOM_ID;

    /**
     * Sampling rate. If less than 1, it is considered to be a relative value.
     */
    double sampling;

    /**
     * Number of samples to draw (i.e. iterations).
     */
    int numsamples;

    /**
     * Keep the previous medoids in the sample.
     */
    boolean keepmed;

    /**
     * Random factory for initialization.
     */
    RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter numsamplesP = new IntParameter(NUMSAMPLES_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(numsamplesP)) {
        numsamples = numsamplesP.intValue();
      }

      // Larger sample size, used by Schubert and Rousseeuw, 2019
      DoubleParameter samplingP = new DoubleParameter(SAMPLESIZE_ID, 80 + 4. * k) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }

      Flag nokeepmedF = new Flag(NOKEEPMED_ID);
      if(numsamples != 1 && config.grab(nokeepmedF)) {
        keepmed = nokeepmedF.isFalse();
      }

      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected FastCLARA<V> makeInstance() {
      return new FastCLARA<>(distanceFunction, k, maxiter, initializer, fasttol, numsamples, sampling, keepmed, random);
    }
  }
}
