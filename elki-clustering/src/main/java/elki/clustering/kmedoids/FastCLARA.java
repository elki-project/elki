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
package elki.clustering.kmedoids;

import java.util.Random;

import elki.clustering.ClusteringAlgorithmUtil;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Clustering Large Applications (CLARA) with the {@link FastPAM}
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
 * @navassoc - - - elki.data.model.MedoidModel
 *
 * @param <V> Data type
 */
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "preprint, to appear", //
    url = "https://arxiv.org/abs/1810.05691", //
    bibkey = "DBLP:journals/corr/abs-1810-05691")
public class FastCLARA<V> extends FastPAM<V> {
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
  public FastCLARA(Distance<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, double fasttol, int numsamples, double sampling, boolean keepmed, RandomFactory random) {
    super(distanceFunction, k, maxiter, initializer, fasttol);
    this.numsamples = numsamples;
    this.sampling = sampling;
    this.random = random;
    this.keepmed = keepmed;
  }

  @Override
  public Clustering<MedoidModel> run(Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<V> distQ = relation.getDistanceQuery(getDistance());
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
    if(bestmedoids == null) {
      throw new IllegalStateException("numsamples must be larger than 0.");
    }

    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, bestclusters, k);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>();
    Metadata.of(result).setLongName("CLARA Clustering");
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
  public static class Par<V> extends FastPAM.Par<V> {
    /**
     * The number of samples to run.
     */
    public static final OptionID NUMSAMPLES_ID = CLARA.Par.NUMSAMPLES_ID;

    /**
     * The sample size.
     */
    public static final OptionID SAMPLESIZE_ID = CLARA.Par.SAMPLESIZE_ID;

    /**
     * Draw independent samples.
     */
    public static final OptionID NOKEEPMED_ID = CLARA.Par.NOKEEPMED_ID;

    /**
     * Random generator.
     */
    public static final OptionID RANDOM_ID = CLARA.Par.RANDOM_ID;

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
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(NUMSAMPLES_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> numsamples = x);
      // Larger sample size, used by Schubert and Rousseeuw, 2019
      new DoubleParameter(SAMPLESIZE_ID, 80 + 4. * k) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> sampling = x);
      if(numsamples > 1) {
        new Flag(NOKEEPMED_ID).grab(config, x -> keepmed = !x);
      }
      new RandomParameter(RANDOM_ID).grab(config, x -> random = x);
    }

    @Override
    public FastCLARA<V> make() {
      return new FastCLARA<>(distanceFunction, k, maxiter, initializer, fasttol, numsamples, sampling, keepmed, random);
    }
  }
}
