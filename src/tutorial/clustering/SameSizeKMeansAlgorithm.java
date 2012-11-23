package tutorial.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.AbstractKMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansPlusPlusInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * K-means variation that produces equally sized clusters.
 * 
 * Note that this is a rather obvious variation, and one cannot expect very good
 * results from this algorithm. K-means already is quite primitive, and putting
 * in the size constraint will likely not make the results much better (in
 * particular, it will even less be able to make sense of outliers!)
 * 
 * There is no reference for this algorithm. If you want to cite it, please cite
 * the latest ELKI release as given on the ELKI web page:
 * http://elki.dbs.ifi.lmu.de/wiki/Releases
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class SameSizeKMeansAlgorithm<V extends NumberVector<?>> extends AbstractKMeans<V, DoubleDistance, MeanModel<V>> {
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
  public SameSizeKMeansAlgorithm(PrimitiveDoubleDistanceFunction<? super NumberVector<?>> distanceFunction, int k, int maxiter, KMeansInitialization<V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  /**
   * Run k-means with cluster size constraints.
   * 
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  public Clustering<MeanModel<V>> run(Database database, Relation<V> relation) {
    // Database objects to process
    final DBIDs ids = relation.getDBIDs();
    // Our desired cluster size:
    final int minsize = ids.size() / k; // rounded down
    final int maxsize = (ids.size() + k - 1) / k; // rounded up
    // Choose initial means
    List<? extends NumberVector<?>> means = initializer.chooseInitialMeans(relation, k, getDistanceFunction());
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<ModifiableDBIDs>();
    for (int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(maxsize));
    }

    // This is a safe cast - see constructor.
    @SuppressWarnings("unchecked")
    PrimitiveDoubleDistanceFunction<NumberVector<?>> df = (PrimitiveDoubleDistanceFunction<NumberVector<?>>) getDistanceFunction();
    // Temporary data storage
    final WritableDataStore<Meta> metas = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Meta.class);
    initializeMeta(relation, means, metas, df);
    
    // Build a sorted list of objects, by descending distance delta
    ArrayModifiableDBIDs tids = DBIDUtil.newArray(ids);
    {
      // Comparator: sort by largest benefit of assigning to preferred cluster.
      final Comparator<DBIDRef> comp = new Comparator<DBIDRef>() {
        @Override
        public int compare(DBIDRef o1, DBIDRef o2) {
          Meta c1 = metas.get(o1), c2 = metas.get(o2);
          return -Double.compare(c1.priority(), c2.priority());
        }
      };

      // Initialization phase:
      int start = 0;
      while (start < tids.size()) {
        tids.sort(start, tids.size(), comp);
        DBIDArrayIter id = tids.iter();
        id.seek(start);
        while (id.valid()) {
          Meta c = metas.get(id);
          // Assigning to best cluster - which cannot be empty yet!
          ModifiableDBIDs cluster = clusters.get(c.primary);
          cluster.add(id);
          start++;
          id.advance();
          // Now the cluster may have become empty:
          if (cluster.size() == maxsize) {
            int full = c.primary;
            // A cluster has been fully consumed.
            // Refresh the not yet assigned objects where necessary:
            for (; id.valid(); id.advance()) {
              Meta ca = metas.get(id);
              if (ca.primary == full) {
                // Update the best index:
                for (int i = 0; i < k; i++) {
                  if (i == full || clusters.get(i).size() >= maxsize) {
                    continue;
                  }
                  if (ca.primary == full || ca.dists[i] < ca.dists[ca.primary]) {
                    ca.primary = i;
                  }
                }
                metas.put(id, ca); // Changed.
              } else if (ca.secondary == full) {
                // Update the worst index:
                for (int i = 0; i < k; i++) {
                  if (i == full || clusters.get(i).size() >= maxsize) {
                    continue;
                  }
                  if (ca.secondary == full || ca.dists[i] > ca.dists[ca.secondary]) {
                    ca.secondary = i;
                  }
                }
                metas.put(id, ca); // Changed.
              }
            }
            // The next iteration will perform the sorting!
          }
        }
        // Note: we expect Candidate.a == cluster the object is assigned to!
      }
    }
    // Recompute means.
    means = means(clusters, means, relation);

    {
      // Iteration phase.
      // Comparator: sort by largest gain by transfer
      final Comparator<DBIDRef> comp = new Comparator<DBIDRef>() {
        @Override
        public int compare(DBIDRef o1, DBIDRef o2) {
          Meta c1 = metas.get(o1), c2 = metas.get(o2);
          return Double.compare(c1.priority(), c2.priority());
        }
      };
      // List for sorting cluster preferences
      int[] preferences = new int[k];
      for (int i = 0; i < k; i++) {
        preferences[i] = i;
      }
      // Comparator for this list.
      final PreferenceComparator pcomp = new PreferenceComparator();

      // Initialize transfer lists:
      ArrayModifiableDBIDs[] transfers = new ArrayModifiableDBIDs[k];
      for (int i = 0; i < k; i++) {
        transfers[i] = DBIDUtil.newArray();
      }

      for (int j = 0; maxiter < 0 || j < maxiter; j++) {
        int active = 0;
        updateDistances(relation, means, metas, df);
        tids.sort(comp);
        for (DBIDIter id = tids.iter(); id.valid(); id.advance()) {
          Meta c = metas.get(id);
          ModifiableDBIDs source = clusters.get(c.primary);
          boolean transferred = false;
          pcomp.c = c;
          IntegerArrayQuickSort.sort(preferences, pcomp);
          for (int i : preferences) {
            if (i == c.primary) {
              continue;
            }
            ModifiableDBIDs dest = clusters.get(i);
            // Can we pair this transfer?
            if (!transfers[i].isEmpty()) {
              DBID other = transfers[i].get(0);
              Meta c2 = metas.get(other);
              if (c.gain(i) + c2.gain(c.primary) > 0) {
                transfers[i].remove(0);
                transfer(metas, c2, dest, source, other, c.primary);
                transfer(metas, c, source, dest, id, i);
                active += 2;
                transferred = true;
                break;
              }
            }
            // If cluster sizes allow, move a single object.
            if (c.gain(i) > 0 && (dest.size() < maxsize && source.size() > minsize)) {
              transfer(metas, c, source, dest, id, i);
              active += 1;
              transferred = true;
              break;
            }
          }
          // If the object would prefer a different cluster, put in outgoing
          // transfer list.
          if (!transferred && (c.dists[c.primary] > c.dists[c.secondary])) {
            transfers[c.primary].add(id);
          }
        }
        // TODO: try to get more transfers out of the transfer lists done by
        // considering more than one object?
        int pending = 0;
        // Clear transfer lists for next iteration.
        for (int i = 0; i < k; i++) {
          pending += transfers[i].size();
          transfers[i].clear();
        }
        if (LOG.isDebuggingFine()) {
          LOG.debugFine("Performed " + active + " transfers in iteration " + j + " skipped " + pending);
        }
        if (active <= 0) {
          break;
        }
        // Recompute means after reassignment
        means = means(clusters, means, relation);
      }
    }

    // Wrap result
    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    Clustering<MeanModel<V>> result = new Clustering<MeanModel<V>>("k-Means Samesize Clustering", "kmeans-samesize-clustering");
    for (int i = 0; i < clusters.size(); i++) {
      MeanModel<V> model = new MeanModel<V>(factory.newNumberVector(means.get(i).getColumnVector().getArrayRef()));
      result.addCluster(new Cluster<MeanModel<V>>(clusters.get(i), model));
    }
    return result;
  }

  protected void transfer(final WritableDataStore<Meta> metas, Meta meta, ModifiableDBIDs src, ModifiableDBIDs dst, DBIDRef id, Integer dstnum) {
    dst.add(id);
    src.remove(id);
    meta.primary = dstnum;
    metas.put(id, meta); // Make sure the storage is up to date.
  }

  protected void initializeMeta(Relation<V> relation, List<? extends NumberVector<?>> means, final WritableDataStore<Meta> metas, PrimitiveDoubleDistanceFunction<NumberVector<?>> df) {
    // Build the metadata, track the two nearest cluster centers.
    for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
      Meta c = new Meta(k);
      V fv = relation.get(id);
      for (int i = 0; i < k; i++) {
        c.dists[i] = df.doubleDistance(fv, means.get(i));
        if (i > 0) {
          if (c.dists[i] < c.dists[c.primary]) {
            c.primary = i;
          } else if (c.dists[i] > c.dists[c.secondary]) {
            c.secondary = i;
          }
        }
      }
      metas.put(id, c);
    }
  }

  protected void updateDistances(Relation<V> relation, List<? extends NumberVector<?>> means, final WritableDataStore<Meta> metas, PrimitiveDoubleDistanceFunction<NumberVector<?>> df) {
    for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
      Meta c = metas.get(id);
      V fv = relation.get(id);
      // Update distances to means.
      c.secondary = -1;
      for (int i = 0; i < k; i++) {
        c.dists[i] = df.doubleDistance(fv, means.get(i));
        if (c.primary != i) {
          if (c.secondary < 0 || c.dists[i] < c.dists[c.secondary]) {
            c.secondary = i;
          }
        }
      }
      metas.put(id, c); // Changed.
    }
  }

  /**
   * Object metadata.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Meta {
    /**
     * Distance to the cluster centers.
     */
    double[] dists;

    /**
     * Indexes: primary assignment (current or best), secondary assignment
     * (second best or worst). The actual meanin changes from initialization to
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
     * Gain from switching to cluster i.
     * 
     * @param i Target cluster
     * @return Gain
     */
    protected double gain(Integer i) {
      return dists[primary] - dists[i];
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
  }

  /**
   * Sort a list of integers (= cluster numbers) by the distances.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public class PreferenceComparator implements IntegerComparator {
    /**
     * Meta to use for comparison.
     */
    Meta c = null;

    @Override
    public int compare(int o1, int o2) {
      return Double.compare(c.dists[o1], c.dists[o2]);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
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
    protected KMeansInitialization<V> initializer;

    /**
     * Distance function
     */
    protected PrimitiveDoubleDistanceFunction<? super NumberVector<?>> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDoubleDistanceFunction<? super NumberVector<?>>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, PrimitiveDoubleDistanceFunction.class);
      if (config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(new GreaterConstraint(1));
      if (config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<KMeansInitialization<V>>(INIT_ID, KMeansInitialization.class, KMeansPlusPlusInitialMeans.class);
      if (config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(MAXITER_ID, -1);
      maxiterP.addConstraint(new GreaterEqualConstraint(-1));
      if (config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected SameSizeKMeansAlgorithm<V> makeInstance() {
      return new SameSizeKMeansAlgorithm<V>(distanceFunction, k, maxiter, initializer);
    }
  }
}
