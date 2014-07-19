package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * CLARA is a clustering method for large data sets based on PAM, partitioning
 * around medoids ({@link KMedoidsPAM}) based on sampling.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector type
 */
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
    int sampleSize = (int) ((sampling < 1.) ? sampling * ids.size() : sampling);
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction());

    double best = Double.POSITIVE_INFINITY;
    ArrayModifiableDBIDs bestmedoids = null;
    List<ModifiableDBIDs> bestclusters = null;

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Random samples.", numsamples, LOG) : null;
    for(int j = 0; j < numsamples; j++) {
      DBIDs rids = DBIDUtil.randomSample(ids, sampleSize, random);
      // Choose initial medoids
      ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, rids, distQ));
      // Setup cluster assignment store
      List<ModifiableDBIDs> clusters = new ArrayList<>();
      for(int i = 0; i < k; i++) {
        clusters.add(DBIDUtil.newHashSet(relation.size() / k));
      }
      runPAMOptimization(distQ, rids, medoids, clusters);
      double score = assignToNearestCluster(medoids, ids, clusters, distQ);
      if(score < best) {
        best = score;
        bestmedoids = medoids;
        bestclusters = clusters;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("CLARA Clustering", "clara-clustering");
    for(int i = 0; i < bestclusters.size(); i++) {
      MedoidModel model = new MedoidModel(bestmedoids.get(i));
      result.addToplevelCluster(new Cluster<>(bestclusters.get(i), model));
    }
    return result;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means Object centroids
   * @param ids Object ids
   * @param clusters cluster assignment
   * @param distQ distance query
   * @return Sum of distances.
   */
  protected double assignToNearestCluster(ArrayDBIDs means, DBIDs ids, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V> distQ) {
    double distsum = 0.;
    DBIDArrayIter miter = means.iter();
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
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
      distsum += mindist * mindist;
      if(clusters.get(minIndex).add(iditer)) {
        // Remove from previous cluster
        // TODO: keep a list of cluster assignments to save this search?
        for(int j = 0; j < k; j++) {
          if(j != minIndex && clusters.get(j).remove(iditer)) {
            break;
          }
        }
      }
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
      IntParameter numsamplesP = new IntParameter(NUMSAMPLES_ID, 1) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(numsamplesP)) {
        numsamples = numsamplesP.intValue();
      }

      DoubleParameter samplingP = new DoubleParameter(SAMPLESIZE_ID, 1) //
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
