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

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo-k-Means variations, that assigns each object to the nearest center.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
public class SingleAssignmentKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SingleAssignmentKMeans.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = SingleAssignmentKMeans.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param initializer Initialization method
   */
  public SingleAssignmentKMeans(NumberVectorDistanceFunction<? super V> distanceFunction, int k, KMeansInitialization<? super V> initializer) {
    super(distanceFunction, k, -1, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Assignment", "kmeans-assignment");
    }
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    List<Vector> means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];

    assignToNearestCluster(relation, means, clusters, assignment, varsum);

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("Nearest Centroid Clustering", "nearest-center-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      KMeansModel model = new KMeansModel(means.get(i), varsum[i]);
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
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      // Do NOT invoke super.makeOptions, as we don't want to have the maxiter
      // parameter, nor a warning for other distance functions.
      getParameterDistanceFunction(config);
      getParameterK(config);
      getParameterInitialization(config);
    }

    @Override
    protected SingleAssignmentKMeans<V> makeInstance() {
      return new SingleAssignmentKMeans<>(distanceFunction, k, initializer);
    }
  }
}
