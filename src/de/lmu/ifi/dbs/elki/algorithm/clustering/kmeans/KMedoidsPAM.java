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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides the k-medoids clustering algorithm, using the
 * "Partitioning Around Medoids" approach.
 * 
 * Reference:
 * <p>
 * Clustering my means of Medoids<br />
 * Kaufman, L. and Rousseeuw, P.J.<br />
 * in: Statistical Data Analysis Based on the L_1–Norm and Related Methods
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 * 
 * @param <V> vector datatype
 * @param <D> distance value type
 */
@Title("Partioning Around Medoids")
@Reference(title = "Clustering my means of Medoids", authors = "Kaufman, L. and Rousseeuw, P.J.", booktitle = "Statistical Data Analysis Based on the L_1–Norm and Related Methods")
public class KMedoidsPAM<V, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAM.class);

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
  public KMedoidsPAM(PrimitiveDistanceFunction<? super V, D> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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
    DBIDs ids = relation.getDBIDs();
    // Choose initial medoids
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, distQ));
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<ModifiableDBIDs>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }

    WritableDoubleDataStore second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    assignToNearestCluster(medoids, ids, second, clusters, distQ);

    // Swap phase
    boolean changed = true;
    while(changed) {
      changed = false;
      // Try to swap the medoid with a better cluster member:
      double best = 0;
      DBID bestid = null;
      int bestcluster = -1;
      int i = 0;
      for(DBIDIter miter = medoids.iter(); miter.valid(); miter.advance(), i++) {
        for(DBIDIter iter = clusters.get(i).iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(miter, iter)) {
            continue;
          }
          // double disti = distQ.distance(id, med).doubleValue();
          double cost = 0;
          DBIDIter olditer = medoids.iter();
          for(int j = 0; j < k; j++, olditer.advance()) {
            for(DBIDIter iter2 = clusters.get(j).iter(); iter2.valid(); iter2.advance()) {
              double distcur = distQ.distance(iter2, olditer).doubleValue();
              double distnew = distQ.distance(iter2, iter).doubleValue();
              if(j == i) {
                // Cases 1 and 2.
                double distsec = second.doubleValue(iter2);
                if(distcur > distsec) {
                  // Case 1, other would switch to a third medoid
                  cost += distsec - distcur; // Always positive!
                }
                else { // Would remain with the candidate
                  cost += distnew - distcur; // Could be negative
                }
              }
              else {
                // Cases 3-4: objects from other clusters
                if(distcur < distnew) {
                  // Case 3: no change
                }
                else {
                  // Case 4: would switch to new medoid
                  cost += distnew - distcur; // Always negative
                }
              }
            }
          }
          if(cost < best) {
            best = cost;
            bestid = DBIDUtil.deref(iter);
            bestcluster = i;
          }
        }
      }
      if(LOG.isDebugging()) {
        LOG.debug("Best cost: " + best);
      }
      if(bestid != null) {
        changed = true;
        medoids.set(bestcluster, bestid);
      }
      // Reassign
      if(changed) {
        // TODO: can we save some of these recomputations?
        assignToNearestCluster(medoids, ids, second, clusters, distQ);
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
   * @param means Object centroids
   * @param ids Object ids
   * @param second Distance to second nearest medoid
   * @param clusters cluster assignment
   * @param distQ distance query
   * @return true when any object was reassigned
   */
  protected boolean assignToNearestCluster(ArrayDBIDs means, DBIDs ids, WritableDoubleDataStore second, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V, D> distQ) {
    boolean changed = false;

    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      int minIndex = 0;
      double mindist = Double.POSITIVE_INFINITY;
      double mindist2 = Double.POSITIVE_INFINITY;
      {
        int i = 0;
        for(DBIDIter miter = means.iter(); miter.valid(); miter.advance(), i++) {
          double dist = distQ.distance(iditer, miter).doubleValue();
          if(dist < mindist) {
            minIndex = i;
            mindist2 = mindist;
            mindist = dist;
          }
          else if(dist < mindist2) {
            mindist2 = dist;
          }
        }
      }
      if(clusters.get(minIndex).add(iditer)) {
        changed = true;
        // Remove from previous cluster
        // TODO: keep a list of cluster assignments to save this search?
        for(int i = 0; i < k; i++) {
          if(i != minIndex) {
            if(clusters.get(i).remove(iditer)) {
              break;
            }
          }
        }
      }
      second.put(iditer, mindist2);
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
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<KMedoidsInitialization<V>>(KMeans.INIT_ID, KMedoidsInitialization.class, PAMInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, new GreaterEqualConstraint(0), 0);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected KMedoidsPAM<V, D> makeInstance() {
      return new KMedoidsPAM<V, D>(distanceFunction, k, maxiter, initializer);
    }
  }
}