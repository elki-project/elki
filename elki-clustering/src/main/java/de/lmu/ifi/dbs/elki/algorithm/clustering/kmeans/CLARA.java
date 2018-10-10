/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
 * Clustering Large Applications (CLARA) is a clustering method for large data
 * sets based on PAM, partitioning around medoids ({@link KMedoidsPAM}) based on
 * sampling.
 * <p>
 * Reference:
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br>
 * Clustering Large Data Sets<br>
 * Pattern Recognition in Practice
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br>
 * Clustering Large Applications (Program CLARA)<br>
 * Finding Groups in Data: An Introduction to Cluster Analysis
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> Data type
 */
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Clustering Large Data Sets", //
    booktitle = "Pattern Recognition in Practice", //
    url = "https://doi.org/10.1016/B978-0-444-87877-9.50039-X", //
    bibkey = "doi:10.1016/B978-0-444-87877-9.50039-X")
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Clustering Large Applications (Program CLARA)", //
    booktitle = "Finding Groups in Data: An Introduction to Cluster Analysis", //
    url = "https://doi.org/10.1002/9780470316801.ch3", //
    bibkey = "doi:10.1002/9780470316801.ch3")
public class CLARA<V> extends KMedoidsPAM<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CLARA.class);

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
  public CLARA(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, int numsamples, double sampling, boolean keepmed, RandomFactory random) {
    super(distanceFunction, k, maxiter, initializer);
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

    double best = Double.POSITIVE_INFINITY;
    ArrayModifiableDBIDs bestmedoids = null;
    WritableIntegerDataStore bestclusters = null;

    Random rnd = random.getSingleThreadedRandom();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Processing random samples", numsamples, LOG) : null;
    for(int j = 0; j < numsamples; j++) {
      DBIDs rids = randomSample(ids, samplesize, rnd, keepmed ? bestmedoids : null);
      // FIXME: precompute and use a distance matrix for this sample!

      // Choose initial medoids
      ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, rids, distQ));
      // Setup cluster assignment store
      WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      double score = new /* PAM */Instance(distQ, rids, assignment).run(medoids, maxiter) //
          + assignRemainingToNearestCluster(medoids, ids, rids, assignment, distQ);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(getClass().getName() + ".sample-" + j + ".cost", score));
      }
      if(score < best) {
        best = score;
        bestmedoids = medoids;
        bestclusters = assignment;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".cost", best));
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
   * Draw a random sample of the desired size.
   * 
   * @param ids IDs to sample from
   * @param samplesize Sample size
   * @param rnd Random generator
   * @param previous Previous medoids to always include in the sample.
   * @return Sample
   */
  private static DBIDs randomSample(DBIDs ids, int samplesize, Random rnd, DBIDs previous) {
    if(previous == null) {
      return DBIDUtil.randomSample(ids, samplesize, rnd);
    }
    ModifiableDBIDs sample = DBIDUtil.newHashSet(samplesize);
    sample.addDBIDs(previous);
    sample.addDBIDs(DBIDUtil.randomSample(ids, samplesize - previous.size(), rnd));
    // If these two were not disjoint, we can be short of the desired size!
    if(sample.size() < samplesize) {
      // Draw a large enough sample to make sure to be able to fill it now.
      // This can be less random though, because the iterator may impose an
      // order; but this is a rare code path.
      for(DBIDIter it = DBIDUtil.randomSample(ids, samplesize, rnd).iter(); sample.size() < samplesize && it.valid(); it.advance()) {
        sample.add(it);
      }
    }
    return sample;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param means Object centroids
   * @param ids Object ids
   * @param rids Sample that was already assigned
   * @param assignment cluster assignment
   * @param distQ distance query
   * @return Sum of distances.
   */
  protected double assignRemainingToNearestCluster(ArrayDBIDs means, DBIDs ids, DBIDs rids, WritableIntegerDataStore assignment, DistanceQuery<V> distQ) {
    rids = DBIDUtil.ensureSet(rids); // Ensure we have fast contains
    double distsum = 0.;
    DBIDArrayIter miter = means.iter();
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(rids.contains(iditer)) {
        continue;
      }
      double mindist = Double.POSITIVE_INFINITY;
      int minIndex = 0;
      miter.seek(0); // Reuse iterator.
      for(int i = 0; miter.valid(); miter.advance(), i++) {
        double dist = distQ.distance(iditer, miter);
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      distsum += mindist;
      assignment.put(iditer, minIndex);
    }
    return distsum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends KMedoidsPAM.Parameterizer<V> {
    /**
     * The number of samples to run.
     */
    public static final OptionID NUMSAMPLES_ID = new OptionID("clara.samples", "Number of samples (iterations) to run.");

    /**
     * The sample size.
     */
    public static final OptionID SAMPLESIZE_ID = new OptionID("clara.samplesize", "The size of the sample.");

    /**
     * Draw independent samples.
     */
    public static final OptionID NOKEEPMED_ID = new OptionID("clara.independent", "Draw independent samples (default is to keep the previous best medoids in the sample).");

    /**
     * Random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("clara.random", "Random generator seed.");

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

      // Default sample size suggested by Kaufman and Rousseeuw
      DoubleParameter samplingP = new DoubleParameter(SAMPLESIZE_ID, 40 + 2 * k) //
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
    protected CLARA<V> makeInstance() {
      return new CLARA<>(distanceFunction, k, maxiter, initializer, numsamples, sampling, keepmed, random);
    }
  }
}
