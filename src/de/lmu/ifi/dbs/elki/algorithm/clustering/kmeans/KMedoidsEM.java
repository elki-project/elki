package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractPrimitiveDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides the k-medoids clustering algorithm, using a "bulk" variation of the
 * "Partitioning Around Medoids" approach.
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
 * 
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 * 
 * @param <V> vector datatype
 * @param <D> distance value type
 */
public class KMedoidsEM<V, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsEM.class);

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
  public KMedoidsEM(PrimitiveDistanceFunction<? super V, D> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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
      return new Clustering<MedoidModel>("k-Medoids Clustering", "kmedoids-clustering");
    }
    DistanceQuery<V, D> distQ = database.getDistanceQuery(relation, getDistanceFunction());
    // Choose initial medoids
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, distQ));
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<ModifiableDBIDs>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }
    Mean[] mdists = Mean.newArray(k);

    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    assignToNearestCluster(medoids, mdists, clusters, distQ);

    // Swap phase
    boolean changed = true;
    while(changed) {
      changed = false;
      // Try to swap the medoid with a better cluster member:
      int i = 0;
      for (DBIDIter miter = medoids.iter(); miter.valid(); miter.advance(), i++) {
        DBID best = null;
        Mean bestm = mdists[i];
        for(DBIDIter iter = clusters.get(i).iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(miter, iter)) {
            continue;
          }
          Mean mdist = new Mean();
          for(DBIDIter iter2 = clusters.get(i).iter(); iter2.valid(); iter2.advance()) {
            mdist.put(distQ.distance(iter, iter2).doubleValue());
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

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<MedoidModel>("k-Medoids Clustering", "kmedoids-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      MedoidModel model = new MedoidModel(medoids.get(i));
      result.addCluster(new Cluster<MedoidModel>(clusters.get(i), model));
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
  protected boolean assignToNearestCluster(ArrayDBIDs means, Mean[] mdist, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V, D> distQ) {
    boolean changed = false;

    double[] dists = new double[k];
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      int minIndex = 0;
      double mindist = Double.POSITIVE_INFINITY;
      {
        int i = 0;
        for(DBIDIter miter = means.iter(); miter.valid(); miter.advance(), i++) {
          dists[i] = distQ.distance(iditer, miter).doubleValue();
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
        for (int i = 0; i < k; i++) {
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
  public static class Parameterizer<V, D extends NumberDistance<D, ?>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {
    protected int k;

    protected int maxiter;

    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<KMedoidsInitialization<V>>(KMeans.INIT_ID, KMedoidsInitialization.class, PAMInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, new GreaterEqualConstraint(0), 0);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    @Override
    protected KMedoidsEM<V, D> makeInstance() {
      return new KMedoidsEM<V, D>(distanceFunction, k, maxiter, initializer);
    }
  }
}