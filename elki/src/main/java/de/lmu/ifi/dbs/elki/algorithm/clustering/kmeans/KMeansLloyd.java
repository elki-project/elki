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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The standard k-means algorithm, using Lloyd-style bulk iterations.
 *
 * <p>
 * Reference:<br />
 * S. Lloyd<br/>
 * Least squares quantization in PCM<br/>
 * IEEE Transactions on Information Theory 28 (2)<br/>
 * previously published as Bell Telephone Laboratories Paper
 * </p>
 *
 * @author Arthur Zimek
 *
 * @apiviz.landmark
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("K-Means")
@Description("Finds a least-squared partitioning into k clusters.")
@Reference(authors = "S. Lloyd", //
title = "Least squares quantization in PCM", //
booktitle = "IEEE Transactions on Information Theory 28 (2): 129–137.", //
url = "http://dx.doi.org/10.1109/TIT.1982.1056489")
public class KMeansLloyd<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansLloyd.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansLloyd.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansLloyd(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    double[][] means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction());
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      boolean changed = assignToNearestCluster(relation, means, clusters, assignment, varsum);
      logVarstat(varstat, varsum);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      // Recompute means.
      means = means(clusters, means, relation);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      KMeansModel model = new KMeansModel(means[i], varsum[i]);
      result.addToplevelCluster(new Cluster<>(ids, model));
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
    protected KMeansLloyd<V> makeInstance() {
      return new KMeansLloyd<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
