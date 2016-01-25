package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithmUtil;
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Clustering Large Applications (CLARA) is a clustering method for large data
 * sets based on PAM, partitioning around medoids ({@link KMedoidsPAM}) based on
 * sampling.
 * 
 * Reference:
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br />
 * Clustering Large Data Sets (with discussion)<br />
 * in: Pattern Recognition in Practice II
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> Vector type
 */
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
title = "Clustering Large Data Sets (with discussion)", //
booktitle = "Pattern Recognition in Practice II")
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
   * @param random Random generator
   */
  public CLARA(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, int numsamples, double sampling, RandomFactory random) {
    super(distanceFunction, k, maxiter, initializer);
    this.numsamples = numsamples;
    this.sampling = sampling;
    this.random = random;
  }

  @Override
  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("CLARA Clustering", "clara-clustering");
    }
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction());

    double best = Double.POSITIVE_INFINITY;
    ArrayModifiableDBIDs bestmedoids = null;
    WritableIntegerDataStore bestclusters = null;

    Random rnd = random.getSingleThreadedRandom();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Processing random samples", numsamples, LOG) : null;
    for(int j = 0; j < numsamples; j++) {
      DBIDs rids = DBIDUtil.randomSample(ids, sampling, rnd);
      // Choose initial medoids
      ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, rids, distQ));
      // Setup cluster assignment store
      WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      runPAMOptimization(distQ, rids, medoids, assignment);
      double score = assignRemainingToNearestCluster(medoids, ids, rids, assignment, distQ);
      if(score < best) {
        best = score;
        bestmedoids = medoids;
        bestclusters = assignment;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

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

      DoubleParameter samplingP = new DoubleParameter(SAMPLESIZE_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }

      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected CLARA<V> makeInstance() {
      return new CLARA<>(distanceFunction, k, maxiter, initializer, numsamples, sampling, random);
    }
  }
}
