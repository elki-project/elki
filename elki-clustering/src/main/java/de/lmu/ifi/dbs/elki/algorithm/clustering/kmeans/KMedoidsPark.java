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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.ParkInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A k-medoids clustering algorithm, implemented as EM-style bulk algorithm.
 * <p>
 * In contrast to PAM, which will in each iteration update one medoid with one
 * (arbitrary) non-medoid, this implementation follows the EM pattern. In the
 * expectation step, the best medoid from the cluster members is chosen; in the
 * M-step, the objects are reassigned to their nearest medoid.
 * <p>
 * This implementation evolved naturally from EM and k-means algorithms, but
 * apparently a similar approach was published by Park and Jun, and also
 * Reynolds et al. discussed this kind of approach before as a side note.
 * <p>
 * In our experiments, it tends to be much faster than PAM, but also find less
 * good solutions, as the medoids are only chosen from the cluster members. This
 * aligns with findings of Reynolds et al. and can be explained with the
 * requirement of the new medoid to cover the entire cluster.
 * <p>
 * Reference:
 * <p>
 * H.-S. Park, C.-H. Jun<br>
 * A simple and fast algorithm for K-medoids clustering<br>
 * Expert Systems with Applications 36(2)
 * <p>
 * A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith<br>
 * Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering
 * Algorithms<br>
 * J. Math. Model. Algorithms 5(4)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <V> vector datatype
 */
@Reference(authors = "H.-S. Park, C.-H. Jun", //
    title = "A simple and fast algorithm for K-medoids clustering", //
    booktitle = "Expert Systems with Applications 36(2)", //
    url = "https://doi.org/10.1016/j.eswa.2008.01.039", //
    bibkey = "DBLP:journals/eswa/ParkJ09")
@Reference(authors = "A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith", //
    title = "Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering Algorithms", //
    booktitle = "J. Math. Model. Algorithms 5(4)", //
    url = "https://doi.org/10.1007/s10852-005-9022-1", //
    bibkey = "DBLP:journals/jmma/ReynoldsRIR06")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMedoidsEM")
public class KMedoidsPark<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPark.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPark.class.getName();

  /**
   * Holds the value of {@link AbstractKMeans#K_ID}.
   */
  protected int k;

  /**
   * Holds the value of {@link AbstractKMeans#MAXITER_ID}.
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
  public KMedoidsPark(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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
    DistanceQuery<V> distQ = DatabaseUtil.precomputedDistanceQuery(database, relation, getDistanceFunction(), LOG);
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, relation.getDBIDs(), distQ));
    DBIDArrayMIter miter = medoids.iter();
    double[] mdists = new double[k];
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      HashSetModifiableDBIDs set = DBIDUtil.newHashSet(relation.size() / k);
      set.add(miter.seek(i)); // Add medoids.
      clusters.add(set);
    }

    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    double tc = assignToNearestCluster(miter, mdists, clusters, distQ);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
    }

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Medoids EM iteration", LOG) : null;
    // Swap phase
    int iteration = 0;
    DBIDVar best = DBIDUtil.newVar();
    while(true) {
      boolean changed = false;
      // Try to swap the medoid with a better cluster member:
      int i = 0;
      for(miter.seek(0); miter.valid(); miter.advance(), i++) {
        best.unset();
        double bestm = mdists[i];
        for(DBIDIter iter = clusters.get(i).iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(miter, iter)) {
            continue;
          }
          double sum = 0;
          for(DBIDIter iter2 = clusters.get(i).iter(); iter2.valid(); iter2.advance()) {
            if(DBIDUtil.equal(iter, iter2)) {
              continue;
            }
            sum += distQ.distance(iter, iter2);
          }
          if(sum < bestm) {
            best.set(iter);
            bestm = sum;
          }
        }
        if(best.isSet() && !DBIDUtil.equal(miter, best)) {
          changed = true;
          assert (clusters.get(i).contains(best));
          medoids.set(i, best);
          mdists[i] = bestm;
        }
      }
      if(!changed) {
        break; // Converged
      }
      // Reassign
      double nc = assignToNearestCluster(miter, mdists, clusters, distQ);
      ++iteration;
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", nc));
      }
      LOG.incrementProcessed(prog);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("k-Medoids Clustering", "kmedoids-clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      result.addToplevelCluster(new Cluster<>(clusters.get(it.getOffset()), new MedoidModel(DBIDUtil.deref(it))));
    }
    return result;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param miter Iterator over the medoids
   * @param dsum Distance sums
   * @param clusters cluster assignment
   * @param distQ distance query
   * @return cost
   */
  protected double assignToNearestCluster(DBIDArrayIter miter, double[] dsum, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V> distQ) {
    double cost = 0;
    double[] dists = new double[k];
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Find current cluster assignment. Ugly, but is it worth int[n]?
      final int current = currentCluster(clusters, iditer);
      int minindex = -1;
      double mindist = Double.POSITIVE_INFINITY;
      if(current >= 0) {
        // Always prefer current assignment on ties, for convergence.
        minindex = current;
        mindist = dists[current] = distQ.distance(iditer, miter.seek(current));
      }
      for(miter.seek(0); miter.valid(); miter.advance()) {
        if(miter.getOffset() == current) {
          continue;
        }
        double d = dists[miter.getOffset()] = distQ.distance(iditer, miter);
        if(d < mindist) {
          minindex = miter.getOffset();
          mindist = d;
        }
      }
      cost += mindist;
      if(minindex == current) {
        continue;
      }
      if(!clusters.get(minindex).add(iditer)) {
        throw new IllegalStateException("Reassigning to the same cluster. " + current + " -> " + minindex);
      }
      dsum[minindex] += mindist;
      // Remove from previous cluster
      if(current >= 0) {
        if(!clusters.get(current).remove(iditer)) {
          throw new IllegalStateException("Removing from the wrong cluster.");
        }
        dsum[current] -= dists[current];
      }
    }
    return cost;
  }

  /**
   * Find the current cluster assignment.
   *
   * @param clusters Clusters
   * @param id Current object
   * @return Current cluster assignment.
   */
  protected int currentCluster(List<? extends ModifiableDBIDs> clusters, DBIDRef id) {
    for(int i = 0; i < k; i++) {
      if(clusters.get(i).contains(id)) {
        return i;
      }
    }
    return -1;
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
    protected int k;

    protected int maxiter;

    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<>(KMeans.INIT_ID, KMedoidsInitialization.class, ParkInitialMeans.class);
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
    protected KMedoidsPark<V> makeInstance() {
      return new KMedoidsPark<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
