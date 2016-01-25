package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.FarthestPointsInitialMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.PAMInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A k-medoids clustering algorithm, implemented as EM-style bulk algorithm.
 * 
 * In contrast to PAM, which will in each iteration update one medoid with one
 * (arbitrary) non-medoid, this implementation follows the EM pattern. In the
 * expectation step, the best medoid from the cluster members is chosen; in the
 * M-step, the objects are reassigned to their nearest medoid.
 * 
 * We do not have a reference for this algorithm. It borrows ideas from EM and
 * PAM. If needed, you are welcome cite it using the latest ELKI publication
 * (this variation is likely not worth publishing on its own).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 * 
 * @param <V> vector datatype
 */
public class KMedoidsEM<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>>implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsEM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsEM.class.getName();

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
  public KMedoidsEM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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
      return new Clustering<>("k-Medoids Clustering", "kmedoids-clustering");
    }
    DistanceQuery<V> distQ = null;
    // Only enforce a distance matrix for PAM initialization, which is slow.
    if(initializer instanceof PAMInitialMeans) {
      distQ = database.getDistanceQuery(relation, getDistanceFunction(), DatabaseQuery.HINT_OPTIMIZED_ONLY);
      if(distQ == null) {
        LOG.verbose("Adding a distance matrix index to accelerate PAM.");
        PrecomputedDistanceMatrix<V> idx = new PrecomputedDistanceMatrix<V>(relation, getDistanceFunction());
        idx.initialize();
        distQ = idx.getDistanceQuery(getDistanceFunction());
      }
    }
    if(distQ == null) {
      distQ = database.getDistanceQuery(relation, getDistanceFunction());
    }
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, relation.getDBIDs(), distQ));
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }
    Mean[] mdists = Mean.newArray(k);

    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    assignToNearestCluster(medoids, mdists, clusters, distQ);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Medoids iteration", LOG) : null;
    // Swap phase
    int iteration = 0;
    for(boolean changed = true; changed; iteration++) {
      LOG.incrementProcessed(prog);
      changed = false;
      // Try to swap the medoid with a better cluster member:
      int i = 0;
      for(DBIDIter miter = medoids.iter(); miter.valid(); miter.advance(), i++) {
        DBID best = null;
        Mean bestm = mdists[i];
        for(DBIDIter iter = clusters.get(i).iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(miter, iter)) {
            continue;
          }
          Mean mdist = new Mean();
          for(DBIDIter iter2 = clusters.get(i).iter(); iter2.valid(); iter2.advance()) {
            mdist.put(distQ.distance(iter, iter2));
          }
          if(mdist.getMean() < bestm.getMean()) {
            best = DBIDUtil.deref(iter);
            bestm = mdist;
          }
        }
        if(best != null && !DBIDUtil.equal(miter, best)) {
          changed = true;
          medoids.set(i, best);
          mdists[i] = bestm;
        }
      }
      // Reassign
      if(changed) {
        assignToNearestCluster(medoids, mdists, clusters, distQ);
      }
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("k-Medoids Clustering", "kmedoids-clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      MedoidModel model = new MedoidModel(DBIDUtil.deref(it));
      result.addToplevelCluster(new Cluster<>(clusters.get(it.getOffset()), model));
    }
    return result;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means a list of k means
   * @param mdist Mean distances
   * @param clusters cluster assignment
   * @param distQ distance query
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(ArrayDBIDs means, Mean[] mdist, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V> distQ) {
    boolean changed = false;

    double[] dists = new double[k];
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      int minIndex = 0;
      double mindist = Double.POSITIVE_INFINITY;
      {
        int i = 0;
        for(DBIDIter miter = means.iter(); miter.valid(); miter.advance(), i++) {
          dists[i] = distQ.distance(iditer, miter);
          if(dists[i] < mindist) {
            minIndex = i;
            mindist = dists[i];
          }
        }
      }
      if(clusters.get(minIndex).add(iditer)) {
        changed = true;
        mdist[minIndex].put(mindist);
        // Remove from previous cluster
        // TODO: keep a list of cluster assignments to save this search?
        for(int i = 0; i < k; i++) {
          if(i != minIndex) {
            if(clusters.get(i).remove(iditer)) {
              mdist[minIndex].put(dists[i], -1);
              break;
            }
          }
        }
      }
    }
    return changed;
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
    protected int k;

    protected int maxiter;

    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<>(KMeans.INIT_ID, KMedoidsInitialization.class, FarthestPointsInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, 0);
      maxiterP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected KMedoidsEM<V> makeInstance() {
      return new KMedoidsEM<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
