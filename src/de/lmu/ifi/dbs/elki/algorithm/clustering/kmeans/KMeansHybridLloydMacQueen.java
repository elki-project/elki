package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Provides the k-means algorithm, alternating between MacQueen-style
 * incremental processing and Lloyd-Style batch steps.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has KMeansModel
 * 
 * @param <V> vector datatype
 * @param <D> distance value type
 */
public class KMeansHybridLloydMacQueen<V extends NumberVector<?>, D extends Distance<D>> extends AbstractKMeans<V, D, KMeansModel<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansHybridLloydMacQueen.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansHybridLloydMacQueen(PrimitiveDistanceFunction<NumberVector<?>, D> distanceFunction, int k, int maxiter, KMeansInitialization<V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel<V>> run(Database database, Relation<V> relation) {
    if (relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    List<Vector> means = new ArrayList<>(k);
    for (NumberVector<?> nv : initializer.chooseInitialMeans(database, relation, k, getDistanceFunction())) {
      means.add(nv.getColumnVector());
    }
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for (int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    for (int iteration = 0; maxiter <= 0 || iteration < maxiter; iteration += 2) {
      { // MacQueen
        if (prog != null) {
          prog.incrementProcessed(LOG);
        }
        boolean changed = macQueenIterate(relation, means, clusters, assignment);
        if (!changed) {
          break;
        }
      }
      { // Lloyd
        if (prog != null) {
          prog.incrementProcessed(LOG);
        }
        boolean changed = assignToNearestCluster(relation, means, clusters, assignment);
        // Stop if no cluster assignment changed.
        if (!changed) {
          break;
        }
        // Recompute means.
        means = means(clusters, means, relation);
      }
    }
    if (prog != null) {
      prog.setCompleted(LOG);
    }

    // Wrap result
    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    Clustering<KMeansModel<V>> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for (int i = 0; i < clusters.size(); i++) {
      KMeansModel<V> model = new KMeansModel<>(factory.newNumberVector(means.get(i).getColumnVector().getArrayRef()));
      result.addToplevelCluster(new Cluster<>(clusters.get(i), model));
    }
    return result;
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
  public static class Parameterizer<V extends NumberVector<?>, D extends Distance<D>> extends AbstractKMeans.Parameterizer<V, D> {
    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected KMeansHybridLloydMacQueen<V, D> makeInstance() {
      return new KMeansHybridLloydMacQueen<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
