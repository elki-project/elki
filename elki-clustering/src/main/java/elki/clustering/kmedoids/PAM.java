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

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.ClusteringAlgorithmUtil;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.initialization.BUILD;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The original Partitioning Around Medoids (PAM) algorithm or k-medoids
 * clustering, as proposed by Kaufman and Rousseeuw in "Clustering by means of
 * Medoids".
 * <p>
 * Reference:
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br>
 * Clustering by means of Medoids<br>
 * Statistical Data Analysis Based on the L1-Norm and Related Methods
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <V> vector datatype
 */
@Title("Partioning Around Medoids")
@Priority(Priority.IMPORTANT)
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Clustering by means of Medoids", //
    booktitle = "Statistical Data Analysis Based on the L1-Norm and Related Methods", //
    bibkey = "books/misc/KauRou87")
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Partitioning Around Medoids (Program PAM)", //
    booktitle = "Finding Groups in Data: An Introduction to Cluster Analysis", //
    url = "https://doi.org/10.1002/9780470316801.ch2", //
    bibkey = "doi:10.1002/9780470316801.ch2")
public class PAM<V> extends AbstractDistanceBasedAlgorithm<Distance<? super V>, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PAM.class);

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
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public PAM(Distance<? super V> distance, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distance);
    if(k > 0x7FFF) {
      throw new NotImplementedException("PAM supports at most " + 0x7FFF + " clusters.");
    }
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Run k-medoids
   *
   * @param relation relation to use
   * @return result
   */
  public Clustering<MedoidModel> run(Relation<V> relation) {
    DistanceQuery<V> distQ = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = initialMedoids(distQ, ids);

    // Setup cluster assignment store
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();
    run(distQ, ids, medoids, assignment);
    getLogger().statistics(optd.end());

    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, assignment, k);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>();
    Metadata.of(result).setLongName("PAM Clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      result.addToplevelCluster(new Cluster<>(clusters[it.getOffset()], new MedoidModel(DBIDUtil.deref(it))));
    }
    return result;
  }

  /**
   * Choose the initial medoids.
   *
   * @param distQ Distance query
   * @param ids IDs to choose from
   * @return Initial medoids
   */
  protected ArrayModifiableDBIDs initialMedoids(DistanceQuery<V> distQ, DBIDs ids) {
    if(getLogger().isStatistics()) {
      getLogger().statistics(new StringStatistic(getClass().getName() + ".initialization", initializer.toString()));
    }
    Duration initd = getLogger().newDuration(getClass().getName() + ".initialization-time").begin();
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distQ));
    getLogger().statistics(initd.end());
    if(medoids.size() != k) {
      throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
    }
    return medoids;
  }

  /**
   * Run the main algorithm. Internal use, for easier subclassing, this
   * primarily is a wrapper around "new Instance" for subclasses.
   * 
   * @param distQ Distance query
   * @param ids IDs to process
   * @param medoids Current medoids
   * @param assignment Cluster assignment output
   */
  protected void run(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
  }

  /**
   * Instance for a single dataset.
   *
   * Note: we experimented with not caching the distance to nearest and second
   * nearest, but only the assignments. The matrix lookup was more expensive, so
   * this is probably worth the 2*n doubles in storage.
   *
   * @author Erich Schubert
   */
  protected static class Instance {
    /**
     * Ids to process.
     */
    DBIDs ids;

    /**
     * Distance function to use.
     */
    DistanceQuery<?> distQ;

    /**
     * Distance to the nearest medoid of each point.
     */
    WritableDoubleDataStore nearest;

    /**
     * Distance to the second nearest medoid.
     */
    WritableDoubleDataStore second;

    /**
     * Cluster mapping.
     */
    WritableIntegerDataStore assignment;

    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      this.distQ = distQ;
      this.ids = ids;
      this.assignment = assignment;
      this.nearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      this.second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    }

    /**
     * Run the PAM optimization phase.
     *
     * @param medoids Medoids list
     * @param maxiter
     * @return final cost
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      // TODO: reuse distance information, from the build phase, when possible?
      double tc = assignToNearestCluster(medoids);
      String key = getClass().getName();
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".cost", tc));
      }

      final boolean metric = distQ.getDistance().isMetric();

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      DBIDArrayIter m = medoids.iter();
      int iteration = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = Double.POSITIVE_INFINITY;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h)), h)) {
            continue; // This is a medoid.
          }
          final double hdist = nearest.doubleValue(h); // Current cost of h.
          if(metric && hdist <= 0.) {
            continue; // Duplicate of a medoid.
          }
          // Find the best possible swap for h:
          for(int pi = 0; pi < k; pi++) {
            // hdist is the cost we get back by making the non-medoid h medoid.
            final double cpi = computeReassignmentCost(h, pi) - hdist;
            if(cpi < best) {
              best = cpi;
              bestid.set(h);
              bestcluster = pi;
            }
          }
        }
        if(!(best < -1e-12 * tc)) {
          break;
        }
        medoids.set(bestcluster, bestid);
        // Reassign
        double nc = assignToNearestCluster(medoids);
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".cost", nc));
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
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
      }
      return tc;
    }

    /**
     * Compute the reassignment cost of one swap.
     *
     * @param h Current object to swap with the medoid
     * @param mnum Medoid number to be replaced
     * @return cost
     */
    protected double computeReassignmentCost(DBIDRef h, int mnum) {
      double cost = 0.;
      // Compute costs of reassigning other objects j:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        if(DBIDUtil.equal(h, j)) {
          continue;
        }
        // distance(j, i) to nearest medoid
        final double distcur = nearest.doubleValue(j);
        // distance(j, h) to new medoid
        final double dist_h = distQ.distance(h, j);
        // Check if current medoid of j is removed:
        if(assignment.intValue(j) == mnum) {
          // distance(j, o) to second nearest / possible reassignment
          final double distsec = second.doubleValue(j);
          // Case 1b: j switches to new medoid, or to the second nearest:
          cost += Math.min(dist_h, distsec) - distcur;
        }
        else if(dist_h < distcur) {
          // Case 1c: j is closer to h than its current medoid
          cost += dist_h - distcur;
        } // else Case 1a): j is closer to i than h and m, so no change.
      }
      return cost;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those objects, that are nearest to the k<sup>th</sup> mean.
     *
     * @param means Object centroids
     * @return Assignment cost
     */
    protected double assignToNearestCluster(ArrayDBIDs means) {
      DBIDArrayIter miter = means.iter();
      double cost = 0.;
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        double mindist = Double.POSITIVE_INFINITY,
            mindist2 = Double.POSITIVE_INFINITY;
        int minindx = -1;
        for(miter.seek(0); miter.valid(); miter.advance()) {
          final double dist = distQ.distance(iditer, miter);
          if(dist < mindist) {
            mindist2 = mindist;
            minindx = miter.getOffset();
            mindist = dist;
          }
          else if(dist < mindist2) {
            mindist2 = dist;
          }
        }
        if(minindx < 0) {
          throw new AbortException("Too many infinite distances. Cannot assign objects.");
        }
        assignment.put(iditer, minindx);
        nearest.put(iditer, mindist);
        second.put(iditer, mindist2);
        cost += mindist;
      }
      return cost;
    }

  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super V>> {
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
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<KMedoidsInitialization<V>>(KMeans.INIT_ID, KMedoidsInitialization.class, defaultInitializer()) //
          .grab(config, x -> initializer = x);
      new IntParameter(KMeans.MAXITER_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxiter = x);
    }

    /**
     * Default initialization method.
     *
     * @return Initialization method
     */
    @SuppressWarnings("rawtypes")
    protected Class<? extends KMedoidsInitialization> defaultInitializer() {
      return BUILD.class;
    }

    @Override
    public PAM<V> make() {
      return new PAM<>(distance, k, maxiter, initializer);
    }
  }
}
