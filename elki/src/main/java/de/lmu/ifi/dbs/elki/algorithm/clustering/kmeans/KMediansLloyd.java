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
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * k-medians clustering algorithm, but using Lloyd-style bulk iterations instead
 * of the more complicated approach suggested by Kaufman and Rousseeuw (see
 * {@link KMedoidsPAM} instead).
 *
 * Reference:
 * <p>
 * Clustering via Concave Minimization<br />
 * P. S. Bradley, O. L. Mangasarian, W. N. Street<br />
 * in: Advances in Neural Information Processing Systems (NIPS)
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.has MeanModel
 *
 * @param <V> vector datatype
 */
@Title("K-Medians")
@Reference(title = "Clustering via Concave Minimization", //
authors = "P. S. Bradley, O. L. Mangasarian, W. N. Street", //
booktitle = "Advances in Neural Information Processing Systems", //
url = "https://papers.nips.cc/paper/1260-clustering-via-concave-minimization.pdf")
public class KMediansLloyd<V extends NumberVector> extends AbstractKMeans<V, MeanModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMediansLloyd.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMediansLloyd.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMediansLloyd(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<MeanModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Medians Clustering", "kmedians-clustering");
    }
    // Choose initial medians
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    List<Vector> medians = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] distsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Medians iteration", LOG) : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      boolean changed = assignToNearestCluster(relation, medians, clusters, assignment, distsum);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      // Recompute medians.
      medians = medians(clusters, medians, relation);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }
    // Wrap result
    Clustering<MeanModel> result = new Clustering<>("k-Medians Clustering", "kmedians-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      MeanModel model = new MeanModel(medians.get(i));
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
    protected KMediansLloyd<V> makeInstance() {
      return new KMediansLloyd<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
