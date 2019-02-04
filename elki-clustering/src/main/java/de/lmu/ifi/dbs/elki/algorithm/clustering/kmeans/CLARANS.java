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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithmUtil;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * CLARANS: a method for clustering objects for spatial data mining
 * is inspired by PAM (partitioning around medoids, {@link KMedoidsPAM})
 * and CLARA and also based on sampling.
 * <p>
 * This implementation tries to balance memory and computation time.
 * By caching the distances to the two nearest medoids, we usually only need
 * O(n) instead of O(nk) distance computations for one iteration, at
 * the cost of needing O(2n) memory to store them.
 * <p>
 * The implementation is fairly ugly, because we have three solutions (the best
 * found so far, the current solution, and a neighbor candidate); and for each
 * point in each solution we need the best and second best assignments. But with
 * Java 11, we may be able to switch to value types that would clean this code
 * significantly, without the overhead of O(n) objects.
 * <p>
 * Reference:
 * <p>
 * R. T. Ng, J. Han<br>
 * CLARANS: a method for clustering objects for spatial data mining<br>
 * IEEE Transactions on Knowledge and Data Engineering 14(5)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @navassoc - - - de.lmu.ifi.dbs.elki.data.model.MedoidModel
 * @has - - - Assignment
 *
 * @param <V> Vector type
 */
@Reference(authors = "R. T. Ng, J. Han", //
    title = "CLARANS: a method for clustering objects for spatial data mining", //
    booktitle = "IEEE Transactions on Knowledge and Data Engineering 14(5)", //
    url = "https://doi.org/10.1109/TKDE.2002.1033770", //
    bibkey = "DBLP:journals/tkde/NgH02")
public class CLARANS<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CLARANS.class);

  /**
   * Number of clusters to find.
   */
  int k;

  /**
   * Number of samples to draw (i.e. restarts).
   */
  int numlocal;

  /**
   * Sampling rate. If less than 1, it is considered to be a relative value.
   */
  double maxneighbor;

  /**
   * Random factory for initialization.
   */
  RandomFactory random;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param k Number of clusters to produce
   * @param numlocal Number of samples (restarts)
   * @param maxneighbor Neighbor sampling rate (absolute or relative)
   * @param random Random generator
   */
  public CLARANS(DistanceFunction<? super V> distanceFunction, int k, int numlocal, double maxneighbor, RandomFactory random) {
    super(distanceFunction);
    this.k = k;
    this.numlocal = numlocal;
    this.maxneighbor = maxneighbor;
    this.random = random;
  }

  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("CLARANS Clustering", "clarans-clustering");
    }
    if(k * 2 >= relation.size()) {
      // Random sampling of non-medoids will be slow for huge k
      LOG.warning("A very large k was chosen. This implementation is not optimized for this case.");
    }
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction());
    final boolean metric = getDistanceFunction().isMetric();

    // Number of retries, relative rate, or absolute count:
    final int retries = (int) Math.ceil(maxneighbor < 1 ? maxneighbor * k * (ids.size() - k) : maxneighbor);
    Random rnd = random.getSingleThreadedRandom();
    DBIDArrayIter cand = DBIDUtil.ensureArray(ids).iter(); // Might copy!

    // Setup cluster assignment store
    Assignment best = new Assignment(distQ, ids, k);
    Assignment curr = new Assignment(distQ, ids, k);
    Assignment scratch = new Assignment(distQ, ids, k);

    // 1. initialize
    double bestscore = Double.POSITIVE_INFINITY;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("CLARANS sampling restarts", numlocal, LOG) : null;
    for(int i = 0; i < numlocal; i++) {
      // 2. choose random initial medoids
      curr.medoids.clear();
      curr.medoids.addDBIDs(DBIDUtil.randomSample(ids, k, rnd));
      // Cost of initial solution:
      double total = curr.assignToNearestCluster();

      // 3. Set j to 1.
      int j = 1;
      step: while(j < retries) {
        // 4 part a. choose a random non-medoid (~ neighbor in G):
        for(int r = 0;; r++) {
          cand.seek(rnd.nextInt(ids.size())); // Random point
          if(curr.nearest.doubleValue(cand) > 0) {
            break; // Good: not a medoid.
          }
          // We may have many duplicate points
          if(metric && curr.second.doubleValue(cand) == 0) {
            ++j; // Cannot yield an improvement if we are metric.
            continue step;
          }
          else if(!metric && !curr.medoids.contains(cand)) {
            // Probably not a good candidate, but try nevertheless
            break;
          }
          if(r >= 1000) {
            throw new AbortException("Failed to choose a non-medoid in 1000 attempts. Choose k << N.");
          }
          // else: this must be the medoid.
        }
        // 4 part b. choose a random medoid to replace:
        final int otherm = rnd.nextInt(k);
        // 5. check lower cost
        double cost = curr.computeCostDifferential(cand, otherm, scratch);
        if(!(cost < -1e-12 * total)) {
          ++j; // 6. try again
          continue;
        }
        total += cost; // cost is negative!
        // Swap:
        Assignment tmp = curr;
        curr = scratch;
        scratch = tmp;
        j = 1;
      }
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(getClass().getName() + ".sample-" + i + ".cost", total));
      }
      // New best:
      if(total < bestscore) {
        // Swap:
        Assignment tmp = curr;
        curr = best;
        best = tmp;
        bestscore = total;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".cost", bestscore));
    }

    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, best.assignment, k);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("CLARANS Clustering", "clarans-clustering");
    for(DBIDArrayIter it = best.medoids.iter(); it.valid(); it.advance()) {
      result.addToplevelCluster(new Cluster<>(clusters[it.getOffset()], new MedoidModel(DBIDUtil.deref(it))));
    }
    return result;
  }

  /**
   * Assignment state.
   * 
   * @author Erich Schubert
   */
  protected static class Assignment {
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
     * Medoid id of the second closest. Needs some more memory, but saves
     * recomputations in the common case where not much changed.
     */
    WritableIntegerDataStore secondid;

    /**
     * Medoids
     */
    ArrayModifiableDBIDs medoids;

    /**
     * Medoid iterator
     */
    DBIDArrayMIter miter;

    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param k Number of medoids
     */
    public Assignment(DistanceQuery<?> distQ, DBIDs ids, int k) {
      this.distQ = distQ;
      this.ids = ids;
      this.medoids = DBIDUtil.newArray(k);
      this.miter = medoids.iter();
      this.assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      this.nearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      this.secondid = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      this.second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    }

    /**
     * Compute the reassignment cost, for one swap.
     *
     * @param h Current object to swap with any medoid.
     * @param mnum Medoid number to swap with h.
     * @param scratch Scratch assignment to fill.
     * @return Cost change
     */
    protected double computeCostDifferential(DBIDRef h, int mnum, Assignment scratch) {
      // Update medoids of scratch copy.
      scratch.medoids.clear();
      scratch.medoids.addDBIDs(medoids);
      scratch.medoids.set(mnum, h);
      double cost = 0;
      // Compute costs of reassigning other objects j:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        if(DBIDUtil.equal(h, j)) {
          scratch.recompute(j, mnum, 0., -1, Double.POSITIVE_INFINITY);
          continue;
        }
        // distance(j, i) to nearest medoid
        final double distcur = nearest.doubleValue(j);
        // distance(j, h) to new medoid
        final double dist_h = distQ.distance(h, j);
        // current assignment of j
        final int jcur = assignment.intValue(j);
        // Check if current medoid of j is removed:
        if(jcur == mnum) {
          // distance(j, o) to second nearest / possible reassignment
          final double distsec = second.doubleValue(j);
          // Case 1b: j switches to new medoid, or to the second nearest:
          if(dist_h < distsec) {
            cost += dist_h - distcur;
            scratch.assignment.putInt(j, mnum);
            scratch.nearest.putDouble(j, dist_h);
            scratch.second.putDouble(j, distsec);
            scratch.secondid.putInt(j, jcur);
          }
          else {
            // Second nearest is the new assignment.
            cost += distsec - distcur;
            // We have to recompute, because we do not know the true new second
            // nearest.
            scratch.recompute(j, mnum, dist_h, jcur, distsec);
          }
        }
        else if(dist_h < distcur) {
          // Case 1c: j is closer to h than its current medoid
          // and the current medoid is not removed (jcur != mnum).
          cost += dist_h - distcur;
          // Second nearest is the previous assignment
          scratch.assignment.putInt(j, mnum);
          scratch.nearest.putDouble(j, dist_h);
          scratch.second.putDouble(j, distcur);
          scratch.secondid.putInt(j, jcur);
        }
        else { // else Case 1a): j is closer to i than h and m, so no change.
          final int jsec = secondid.intValue(j);
          final double distsec = second.doubleValue(j);
          // Second nearest is still valid.
          if(jsec != mnum && distsec <= dist_h) {
            scratch.assignment.putInt(j, jcur);
            scratch.nearest.putDouble(j, distcur);
            scratch.secondid.putInt(j, jsec);
            scratch.second.putDouble(j, distsec);
          }
          else {
            scratch.recompute(j, jcur, distcur, mnum, dist_h);
          }
        }
      }
      return cost;
    }

    /**
     * Recompute the assignment of one point.
     *
     * @param id Point id
     * @param mnum Medoid number for known distance
     * @param known Known distance
     * @return cost
     */
    protected double recompute(DBIDRef id, int mnum, double known, int snum, double sknown) {
      double mindist = mnum >= 0 ? known : Double.POSITIVE_INFINITY,
          mindist2 = Double.POSITIVE_INFINITY;
      int minIndex = mnum, minIndex2 = -1;
      for(int i = 0; miter.seek(i).valid(); i++) {
        if(i == mnum) {
          continue;
        }
        final double dist = i == snum ? sknown : distQ.distance(id, miter);
        if(DBIDUtil.equal(id, miter) || dist < mindist) {
          minIndex2 = minIndex;
          mindist2 = mindist;
          minIndex = i;
          mindist = dist;
        }
        else if(dist < mindist2) {
          minIndex2 = i;
          mindist2 = dist;
        }
      }
      if(minIndex < 0) {
        throw new AbortException("Too many infinite distances. Cannot assign objects.");
      }
      assignment.putInt(id, minIndex);
      nearest.putDouble(id, mindist);
      secondid.putInt(id, minIndex2);
      second.putDouble(id, mindist2);
      return mindist;
    }

    /**
     * Assign each point to the nearest medoid.
     *
     * @return Assignment cost
     */
    protected double assignToNearestCluster() {
      double cost = 0.;
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        cost += recompute(iditer, -1, Double.POSITIVE_INFINITY, -1, Double.POSITIVE_INFINITY);
      }
      return cost;
    }
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
   */
  public static class Parameterizer<V> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * The number of restarts to run.
     */
    public static final OptionID RESTARTS_ID = new OptionID("clara.numlocal", "Number of samples (restarts) to run.");

    /**
     * The number of neighbors to explore.
     */
    public static final OptionID NEIGHBORS_ID = new OptionID("clara.numneighbor", "Number of tries to find a neighbor.");

    /**
     * Random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("clarans.random", "Random generator seed.");

    /**
     * Maximum neighbors to explore. If less than 1, it is considered to be a
     * relative value.
     */
    double maxneighbor;

    /**
     * Number of restarts to do.
     */
    int numlocal;

    /**
     * Number of cluster centers to find.
     */
    int k;

    /**
     * Random factory for initialization.
     */
    RandomFactory random;

    /**
     * Default sampling rate.
     *
     * @return Default sampling rate.
     */
    protected double defaultRate() {
      return 0.0125;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      IntParameter numlocalP = new IntParameter(RESTARTS_ID, 2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(numlocalP)) {
        numlocal = numlocalP.intValue();
      }

      DoubleParameter maxneighborP = new DoubleParameter(NEIGHBORS_ID, defaultRate()) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(maxneighborP)) {
        maxneighbor = maxneighborP.doubleValue();
      }

      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected CLARANS<V> makeInstance() {
      return new CLARANS<>(distanceFunction, k, numlocal, maxneighbor, random);
    }
  }
}
