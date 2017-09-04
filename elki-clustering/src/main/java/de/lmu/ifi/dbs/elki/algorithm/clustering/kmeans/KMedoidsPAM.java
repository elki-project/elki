/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithmUtil;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.PAMInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The original PAM algorithm or k-medoids clustering, as proposed by Kaufman
 * and Rousseeuw in "Partitioning Around Medoids".
 *
 * Reference:
 * <p>
 * Clustering my means of Medoids<br />
 * Kaufman, L. and Rousseeuw, P.J.<br />
 * in: Statistical Data Analysis Based on the L1-Norm and Related Methods
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 *
 * @param <V> vector datatype
 */
@Title("Partioning Around Medoids")
@Reference(title = "Clustering by means of Medoids", //
authors = "Kaufman, L. and Rousseeuw, P.J.", //
booktitle = "Statistical Data Analysis Based on the L1-Norm and Related Methods")
public class KMedoidsPAM<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>>implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAM.class.getName();

  /**
   * The number of clusters to produce.
   */
  protected int k;

  /**
   * The maximum number of iterations.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMedoidsInitialization<V> initializer;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsPAM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Run k-medoids
   *
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("PAM Clustering", "pam-clustering");
    }
    DistanceQuery<V> distQ = DatabaseUtil.precomputedDistanceQuery(database, relation, getDistanceFunction(), LOG);
    DBIDs ids = relation.getDBIDs();
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distQ));
    if(medoids.size() != k) {
      throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
    }

    // Setup cluster assignment store
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    runPAMOptimization(distQ, ids, medoids, assignment);

    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, assignment, k);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("PAM Clustering", "pam-clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      MedoidModel model = new MedoidModel(DBIDUtil.deref(it));
      result.addToplevelCluster(new Cluster<>(clusters[it.getOffset()], model));
    }
    return result;
  }

  /**
   * Run the PAM optimization phase.
   *
   * @param distQ Distance query
   * @param ids IDs to process
   * @param medoids Medoids list
   * @param assignment Cluster assignment
   */
  protected void runPAMOptimization(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    WritableDoubleDataStore nearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableDoubleDataStore second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    // Initial assignment to nearest medoids
    // TODO: reuse distance information, from the build phase, when possible?
    double tc = assignToNearestCluster(medoids, ids, nearest, second, assignment, distQ);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
    }

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
    // Swap phase
    DBIDVar bestid = DBIDUtil.newVar();
    DBIDArrayIter m = medoids.iter();
    int iteration = 1;
    double[] cost = new double[k];
    for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      // Try to swap a non-medoid with a medoid member:
      double best = Double.POSITIVE_INFINITY;
      int bestcluster = -1;
      // Iterate over all non-medoids:
      for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
        final int pm = assignment.intValue(h);
        m.seek(pm);
        double hdist = nearest.doubleValue(h); // Current assignment cost of h.
        if(DBIDUtil.equal(m, h) || hdist <= 0.) {
          continue; // Only consider non-selected items
        }
        // h is a non-medoid currently in cluster of medoid m.
        Arrays.fill(cost, -hdist);
        // Compute costs of reassigning other objects j:
        for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
          if(DBIDUtil.equal(h, j)) {
            continue;
          }
          final int pj = assignment.intValue(j);
          // distance(j, i) for pi == pj
          final double distcur = nearest.doubleValue(j);
          // second nearest, alternative reassignment
          final double distsec = second.doubleValue(j);
          // distance(j, h), the possible reassignment
          final double dist_h = distQ.distance(h, j);
          // We moved this distance computation out compared to original PAM.
          if(dist_h < distcur) {
            for(int pi = 0; pi < k; pi++) {
              if(pi == pj) { // The current nearest is lost.
                cost[pi] += ((dist_h < distsec) ? //
                dist_h // Case 1b1) j is closer to h
                : distsec // Case 1b2) j would switch to its second nearest
                ) - distcur;
              }
              else { // Case 1c) j is closer to h than its current medoid
                cost[pi] += dist_h - distcur;
              } // else Case 1a): j is closer to i than h and m, so no change.
            }
          }
          else { // Only need to consider pi == pj
            if(dist_h < distsec) {
              // Case 1b1) j is closer to h
              cost[pj] += dist_h - distcur;
            }
            else {
              // Case 1b2) j would switch to its second nearest
              cost[pj] += distsec - distcur;
            }
          }
        }

        // Consider all possible swaps:
        for(int pi = 0; pi < k; pi++) {
          if(cost[pi] < best) {
            best = cost[pi];
            bestid.set(h);
            bestcluster = pi;
          }
        }
      }
      if(best >= 0.) {
        break;
      }
      medoids.set(bestcluster, bestid);
      // Reassign
      double nc = assignToNearestCluster(medoids, ids, nearest, second, assignment, distQ);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", nc));
      }
      if(nc > tc) {
        if(nc - tc < 1e-7 * tc) {
          LOG.warning("PAM failed to converge (numerical instability?)");
          break;
        }
        LOG.warning("PAM failed to converge: costs increased by: " + (nc - tc) + " exepected a decrease by " + best);
        break;
      }
      tc = nc;
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
      LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
    }
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param means Object centroids
   * @param ids Object ids
   * @param nearest Distance to nearest medoid
   * @param second Distance to second nearest medoid
   * @param assignment Cluster assignment
   * @param distQ distance query
   * @return Assignment cost
   */
  protected double assignToNearestCluster(ArrayDBIDs means, DBIDs ids, WritableDoubleDataStore nearest, WritableDoubleDataStore second, WritableIntegerDataStore assignment, DistanceQuery<V> distQ) {
    assert(means.size() == k);
    DBIDArrayIter miter = means.iter();
    double cost = 0.;
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY,
          mindist2 = Double.POSITIVE_INFINITY;
      int minIndex = -1;
      for(int i = 0; i < k; i++) {
        double dist = distQ.distance(iditer, miter.seek(i));
        if(dist < mindist) {
          mindist2 = mindist;
          mindist = dist;
          minIndex = i;
        }
        else if(dist < mindist2) {
          mindist2 = dist;
        }
      }
      if(minIndex < 0) {
        throw new AbortException("Too many infinite distances. Cannot assign objects.");
      }
      assignment.put(iditer, minIndex);
      nearest.put(iditer, mindist);
      second.put(iditer, mindist2);
      cost += mindist;
    }
    return cost;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * The number of clusters to produce.
     */
    protected int k;

    /**
     * The maximum number of iterations.
     */
    protected int maxiter;

    /**
     * Method to choose initial means.
     */
    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<>(KMeans.INIT_ID, KMedoidsInitialization.class, PAMInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, 0) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected KMedoidsPAM<V> makeInstance() {
      return new KMedoidsPAM<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
