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
package tutorial.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.AbstractKMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansPlusPlusInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import it.unimi.dsi.fastutil.ints.IntComparator;

/**
 * K-means variation that produces equally sized clusters.
 * <p>
 * Note that this is a rather obvious variation, and one cannot expect very good
 * results from this algorithm. K-means already is quite primitive, and putting
 * in the size constraint will likely not make the results much better (in
 * particular, it will even less be able to make sense of outliers!)
 * <p>
 * There is no reference for this algorithm. If you want to cite it, please cite
 * the latest ELKI release as given on the ELKI web page:
 * https://elki-project.github.io/publications
 *
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @has - - - Meta
 * @has - - - PreferenceComparator
 *
 * @param <V> Vector type
 */
public class SameSizeKMeansAlgorithm<V extends NumberVector> extends AbstractKMeans<V, MeanModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SameSizeKMeansAlgorithm.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k K parameter
   * @param maxiter Maximum number of iterations
   * @param initializer
   */
  public SameSizeKMeansAlgorithm(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  /**
   * Run k-means with cluster size constraints.
   *
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  @Override
  public Clustering<MeanModel> run(Database database, Relation<V> relation) {
    // Database objects to process
    final DBIDs ids = relation.getDBIDs();
    // Choose initial means
    double[][] means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction());
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k + 2));
    }

    // Meta data storage
    final WritableDataStore<Meta> metas = initializeMeta(relation, means);
    // Perform the initial assignment
    ArrayModifiableDBIDs tids = initialAssignment(clusters, metas, ids);
    // Recompute the means after the initial assignment
    means = means(clusters, means, relation);
    // Refine the result via k-means like iterations
    means = refineResult(relation, means, clusters, metas, tids);

    // Wrap result
    Clustering<MeanModel> result = new Clustering<>("k-Means Samesize Clustering", "kmeans-samesize-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      result.addToplevelCluster(new Cluster<>(clusters.get(i), new MeanModel(means[i])));
    }
    return result;
  }

  /**
   * Initialize the metadata storage.
   *
   * @param relation Relation to process
   * @param means Mean vectors
   * @return Initialized storage
   */
  protected WritableDataStore<Meta> initializeMeta(Relation<V> relation, double[][] means) {
    NumberVectorDistanceFunction<? super V> df = getDistanceFunction();
    // The actual storage
    final WritableDataStore<Meta> metas = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Meta.class);
    // Build the metadata, track the two nearest cluster centers.
    for(DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
      Meta c = new Meta(k);
      V fv = relation.get(id);
      for(int i = 0; i < k; i++) {
        final double d = c.dists[i] = df.distance(fv, DoubleVector.wrap(means[i]));
        if(i > 0) {
          if(d < c.dists[c.primary]) {
            c.primary = i;
          }
          else if(d > c.dists[c.secondary]) {
            c.secondary = i;
          }
        }
      }
      metas.put(id, c);
    }
    return metas;
  }

  protected ArrayModifiableDBIDs initialAssignment(List<ModifiableDBIDs> clusters, final WritableDataStore<Meta> metas, DBIDs ids) {
    // Build a sorted list of objects, by descending distance delta
    ArrayModifiableDBIDs tids = DBIDUtil.newArray(ids);
    // Our desired cluster size:
    final int maxsize = (tids.size() + k - 1) / k; // rounded up
    // Comparator: sort by largest benefit of assigning to preferred cluster.
    final Comparator<DBIDRef> comp = new Comparator<DBIDRef>() {
      @Override
      public int compare(DBIDRef o1, DBIDRef o2) {
        Meta c1 = metas.get(o1), c2 = metas.get(o2);
        return -Double.compare(c1.priority(), c2.priority());
      }
    };
    // We will use this iterator below. It allows seeking!
    DBIDArrayIter id = tids.iter();

    // Initialization phase:
    for(int start = 0; start < tids.size();) {
      tids.sort(start, tids.size(), comp);
      for(id.seek(start); id.valid(); id.advance()) {
        Meta c = metas.get(id);
        // Assigning to best cluster - which cannot be full yet!
        ModifiableDBIDs cluster = clusters.get(c.primary);
        assert (cluster.size() <= maxsize);
        cluster.add(id);
        start++;
        // Now the cluster may have become completely filled:
        if(cluster.size() == maxsize) {
          final int full = c.primary;
          // Refresh the not yet assigned objects where necessary:
          for(id.advance(); id.valid(); id.advance()) {
            Meta ca = metas.get(id);
            if(ca.primary == full) {
              // Update the best index:
              for(int i = 0; i < k; i++) {
                if(i == full || clusters.get(i).size() >= maxsize) {
                  continue;
                }
                if(ca.primary == full || ca.dists[i] < ca.dists[ca.primary]) {
                  ca.primary = i;
                }
              }
              metas.put(id, ca); // Changed.
            }
          }
          // The next iteration will perform the sorting!
          break; // not really necessary - iterator is at end anyway.
        }
      }
      // Note: we expect Candidate.a == cluster the object is assigned to!
    }
    return tids;
  }

  /**
   * Compute the distances of each object to all means. Update
   * {@link Meta#secondary} to point to the best cluster number except the
   * current cluster assignment
   *
   * @param relation Data relation
   * @param means Means
   * @param metas Metadata storage
   * @param df Distance function
   */
  protected void updateDistances(Relation<V> relation, double[][] means, final WritableDataStore<Meta> metas, NumberVectorDistanceFunction<? super V> df) {
    for(DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
      Meta c = metas.get(id);
      V fv = relation.get(id);
      // Update distances to means.
      c.secondary = -1;
      for(int i = 0; i < k; i++) {
        c.dists[i] = df.distance(fv, DoubleVector.wrap(means[i]));
        if(c.primary != i) {
          if(c.secondary < 0 || c.dists[i] < c.dists[c.secondary]) {
            c.secondary = i;
          }
        }
      }
      metas.put(id, c); // Changed.
    }
  }

  /**
   * Perform k-means style iterations to improve the clustering result.
   *
   * @param relation Data relation
   * @param means Means list
   * @param clusters Cluster list
   * @param metas Metadata storage
   * @param tids DBIDs array
   * @return final means
   */
  protected double[][] refineResult(Relation<V> relation, double[][] means, List<ModifiableDBIDs> clusters, final WritableDataStore<Meta> metas, ArrayModifiableDBIDs tids) {
    NumberVectorDistanceFunction<? super V> df = getDistanceFunction();
    // Our desired cluster size:
    final int minsize = tids.size() / k; // rounded down
    final int maxsize = (tids.size() + k - 1) / k; // rounded up

    // Comparator: sort by largest gain by transfer
    final Comparator<DBIDRef> comp = new Comparator<DBIDRef>() {
      @Override
      public int compare(DBIDRef o1, DBIDRef o2) {
        Meta c1 = metas.get(o1), c2 = metas.get(o2);
        return Double.compare(c1.priority(), c2.priority());
      }
    };
    // List for sorting cluster preferences
    final int[] preferences = MathUtil.sequence(0, k);
    // Comparator for this list.
    final PreferenceComparator pcomp = new PreferenceComparator();

    // Initialize transfer lists:
    ArrayModifiableDBIDs[] transfers = new ArrayModifiableDBIDs[k];
    for(int i = 0; i < k; i++) {
      transfers[i] = DBIDUtil.newArray();
    }

    DBIDArrayIter id = tids.iter();
    for(int iter = 0; maxiter <= 0 || iter < maxiter; iter++) {
      updateDistances(relation, means, metas, df);
      tids.sort(comp);
      int active = 0; // Track if anything has changed
      for(id.seek(0); id.valid(); id.advance()) {
        Meta c = metas.get(id);
        IntegerArrayQuickSort.sort(preferences, pcomp.select(c));
        ModifiableDBIDs source = clusters.get(c.primary);
        assert (source.contains(id));
        tloop: for(int i : preferences) {
          if(i == c.primary) {
            continue; // Already assigned here
          }
          ModifiableDBIDs dest = clusters.get(i);
          // Can we pair this transfer?
          final double gain = c.gain(i);
          for(DBIDMIter other = transfers[i].iter(); other.valid(); other.advance()) {
            Meta c2 = metas.get(other);
            if(gain + c2.gain(c.primary) > 0) {
              transfer(metas, c2, dest, source, other, c.primary);
              transfer(metas, c, source, dest, id, i);
              active += 2;
              other.remove(); // last, as this invalidates the reference!
              source = dest; // We are assigned here now.
              continue tloop; // Can try another transfer, with next cluster.
            }
          }
          // If cluster sizes allow, move a single object.
          if(gain > 0 && (dest.size() < maxsize && source.size() > minsize)) {
            transfer(metas, c, source, dest, id, i);
            active += 1;
            source = dest; // We are assigned here now.
            continue tloop;
          }
        }
        // If the object would prefer a different cluster, put in outgoing
        // transfer list.
        if (c.primary != preferences[0] && c.dists[c.primary] > c.dists[preferences[0]]) {
          transfers[c.primary].add(id);
        }
      }
      // TODO: try to get more transfers out of the transfer lists done by
      // considering more than one object?
      int pending = 0;
      // Clear transfer lists for next iteration.
      for(int i = 0; i < k; i++) {
        pending += transfers[i].size();
        transfers[i].clear();
      }
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Iteration #" + iter + ": performed " + active + " transfers skipped " + pending);
      }
      if(active <= 0) {
        break;
      }
      // Recompute means after reassignment
      means = means(clusters, means, relation);
    }
    return means;
  }

  /**
   * Transfer a single element from one cluster to another.
   *
   * @param metas Meta storage
   * @param meta Meta of current object
   * @param src Source cluster
   * @param dst Destination cluster
   * @param id Object ID
   * @param dstnum Destination cluster number
   */
  protected void transfer(final WritableDataStore<Meta> metas, Meta meta, ModifiableDBIDs src, ModifiableDBIDs dst, DBIDRef id, int dstnum) {
    src.remove(id);
    dst.add(id);
    meta.primary = dstnum;
    metas.put(id, meta); // Make sure the storage is up to date.
  }

  /**
   * Object metadata.
   *
   * @author Erich Schubert
   */
  private class Meta {
    /**
     * Distance to the cluster centers.
     */
    double[] dists;

    /**
     * Indexes: primary assignment (current or best), secondary assignment
     * (second best or worst). The actual meaning differs from initialization to
     * iteration phase!
     */
    int primary, secondary;

    /**
     * Constructor.
     *
     * @param k
     */
    protected Meta(int k) {
      dists = new double[k];
      Arrays.fill(dists, Double.POSITIVE_INFINITY);
      primary = 0;
      secondary = 0;
    }

    /**
     * Priority / badness: difference between best and worst. (Assuming that
     * "secondary" is the worst).
     *
     * @return Priority
     */
    protected double priority() {
      return dists[secondary] - dists[primary];
    }

    /**
     * Gain from switching to cluster i.
     *
     * @param i Target cluster
     * @return Gain
     */
    protected double gain(int i) {
      return dists[primary] - dists[i];
    }
  }

  /**
   * Sort a list of integers (= cluster numbers) by the distances.
   *
   * @author Erich Schubert
   */
  public class PreferenceComparator implements IntComparator {
    /**
     * Meta to use for comparison.
     */
    Meta c = null;

    @Override
    public int compare(int o1, int o2) {
      return Double.compare(c.dists[o1], c.dists[o2]);
    }

    /**
     * Set the meta to sort by
     *
     * @param c Meta to sort by
     * @return The comparator
     */
    public IntComparator select(Meta c) {
      this.c = c;
      return this;
    }
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * k Parameter.
     */
    protected int k;

    /**
     * Number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Initialization method.
     */
    protected KMeansInitialization initializer;

    /**
     * Distance function
     */
    protected NumberVectorDistanceFunction<? super V> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<NumberVectorDistanceFunction<? super V>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, NumberVectorDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
        if(!(distanceFunction instanceof EuclideanDistanceFunction) && !(distanceFunction instanceof SquaredEuclideanDistanceFunction)) {
          LOG.warning("k-means optimizes the sum of squares - it should be used with squared euclidean distance and may stop converging otherwise!");
        }
      }

      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMeansInitialization> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, KMeansPlusPlusInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(MAXITER_ID, -1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_MINUSONE_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected SameSizeKMeansAlgorithm<V> makeInstance() {
      return new SameSizeKMeansAlgorithm<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
