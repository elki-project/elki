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
    final WritableDataStore<Candidate> data = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Candidate.class);

    // Build the metadata, track the two nearest cluster centers.
    for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
      Candidate c = new Candidate(k);
      V fv = relation.get(id);
      for (int i = 0; i < k; i++) {
        c.dists[i] = df.doubleDistance(fv, means.get(i));
        if (c.a < 0 || c.dists[i] < c.dists[c.a]) {
          c.b = c.a;
          c.a = i;
        } else if (c.b < 0 || c.dists[i] < c.dists[c.b]) {
          c.b = i;
        }
      }
      data.put(id, c);
    }
    // Build a sorted list of objects, by descending distance delta
    ArrayModifiableDBIDs tids = DBIDUtil.newArray(ids);
    {
      // Comparator: sort by largest benefit of assigning to preferred cluster.
      final Comparator<DBIDRef> comp = new Comparator<DBIDRef>() {
        @Override
        public int compare(DBIDRef o1, DBIDRef o2) {
          Candidate c1 = data.get(o1), c2 = data.get(o2);
          if (c1.b == -1 || c2.b == -1) { // Only one cluster remaining...
            return -Double.compare(-c1.dists[c1.a], -c2.dists[c2.a]);
          } else {
            return -Double.compare(c1.dists[c1.b] - c1.dists[c1.a], c2.dists[c2.b] - c2.dists[c2.a]);
          }
        }
      };

      // Initialization phase:
      int start = 0;
      while (start < tids.size()) {
        tids.sort(start, tids.size(), comp);
        DBIDArrayIter id = tids.iter();
        id.seek(start);
        while (id.valid()) {
          Candidate c = data.get(id);
          // Try best first:
          ModifiableDBIDs cluster = clusters.get(c.a);
          if (cluster.size() < maxsize) {
            cluster.add(id);
            start++;
            id.advance();
          }
          if (cluster.size() == maxsize) {
            // This is the cluster that is now full.
            int full = c.a;
            // A cluster has been fully consumed.
            // Update the candidates where necessary:
            for (; id.valid(); id.advance()) {
              Candidate ca = data.get(id);
              if (ca.a == full) {
                ca.a = ca.b;
                ca.b = -1;
              } else if (ca.b == full) {
                ca.b = -1;
              }
              if (ca.b < 0) {
                // Update the second best index:
                for (int i = 0; i < k; i++) {
                  if (i == full || i == ca.a || clusters.get(i).size() >= maxsize) {
                    continue;
                  }
                  if (ca.b < 0 || ca.dists[i] < ca.dists[ca.b]) {
                    ca.b = i;
                  }
                }
                data.put(id, ca);
              }
            }
          }
        }
        // Note: we want to have Candidate.a == cluster the object is assigned
        // to!
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
          Candidate c1 = data.get(o1), c2 = data.get(o2);
          return Double.compare(c1.dists[c1.b] - c1.dists[c1.a], c2.dists[c2.b] - c2.dists[c2.a]);
        }
      };

      // Initialize transfer lists:
      ArrayModifiableDBIDs[] transfer = new ArrayModifiableDBIDs[k];
      for (int i = 0; i < k; i++) {
        transfer[i] = DBIDUtil.newArray();
      }

      for (int j = 0; maxiter <= 0 || j < maxiter; j++) {
        int active = 0;
        for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
          Candidate c = data.get(id);
          V fv = relation.get(id);
          // Update distances to means; update second
          c.b = -1;
          for (int i = 0; i < k; i++) {
            c.dists[i] = df.doubleDistance(fv, means.get(i));
            if (i != c.a) {
              if (c.b < 0 || c.dists[i] < c.dists[c.b]) {
                c.b = i;
              }
            }
          }
          data.put(id, c);
        }
        tids.sort(comp);
        for (DBIDIter id = tids.iter(); id.valid(); id.advance()) {
          Candidate c = data.get(id);
          ModifiableDBIDs source = clusters.get(c.a);
          boolean transferred = false;
          for (int i = 0; !transferred && i < k; i++) {
            if (i == c.a) {
              continue;
            }
            double gain = c.dists[c.a] - c.dists[i];
            ModifiableDBIDs dest = clusters.get(i);
            // Can we pair this transfer?
            if (!transfer[i].isEmpty()) {
              DBID other = transfer[i].get(0);
              Candidate c2 = data.get(other);
              double gain2 = c2.dists[i] - c2.dists[c.a];
              if (gain + gain2 > 0) {
                transfer[i].remove(0);
                // Move other.
                source.add(other);
                dest.remove(other);
                c2.a = c.a;
                data.put(id, c2);
                // Move current object
                dest.add(id);
                source.remove(id);
                c.a = i;
                data.put(id, c);
                active += 2;
                transferred = true;
                break;
              }
            }
            // If cluster sizes allow, move a single object.
            if (gain > 0 && (dest.size() < maxsize && source.size() > minsize)) {
              dest.add(id);
              source.remove(id);
              c.a = i;
              data.put(id, c);
              active += 1;
              transferred = true;
              break;
            }
          }
          // If the object would prefer a different cluster, put in transfer
          // list.
          if (!transferred && c.dists[c.a] - c.dists[c.b] > 0) {
            transfer[c.a].add(id);
          }
        }
        // TODO: try to get more transfers out of the transfer list done?
        // Clear transfer lists
        int pending = 0;
        for (int i = 0; i < k; i++) {
          pending += transfer[i].size();
          transfer[i].clear();
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

  /**
   * Candidate metadata.
   * 
   * @author Erich Schubert
   */
  private class Candidate {
    double[] dists;

    int a, b;

    private Candidate(int k) {
      dists = new double[k];
      Arrays.fill(dists, Double.POSITIVE_INFINITY);
      a = -1;
      b = -1;
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
    protected int maxiter = 0;

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

      IntParameter maxiterP = new IntParameter(MAXITER_ID, 0);
      maxiterP.addConstraint(new GreaterEqualConstraint(0));
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
