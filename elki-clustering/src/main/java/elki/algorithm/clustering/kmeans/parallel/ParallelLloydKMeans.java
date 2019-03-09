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
package elki.algorithm.clustering.kmeans.parallel;

import elki.algorithm.clustering.ClusteringAlgorithmUtil;
import elki.algorithm.clustering.kmeans.AbstractKMeans;
import elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistanceFunction;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.parallel.ParallelExecutor;
import elki.result.Metadata;

/**
 * Parallel implementation of k-Means clustering.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KMeansProcessor
 *
 * @param <V> Vector type
 */
public class ParallelLloydKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k K parameter
   */
  public ParallelLloydKMeans(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelLloydKMeans.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    double[][] means = initialMeans(database, relation);

    // Store for current cluster assignment.
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];
    KMeansProcessor<V> kmm = new KMeansProcessor<>(relation, distanceFunction, assignment, varsum);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    for(int iteration = 0; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      kmm.nextIteration(means);
      ParallelExecutor.run(ids, kmm);
      // Stop if no cluster assignment changed.
      if(!kmm.changed()) {
        break;
      }
      means = kmm.getMeans();
    }
    LOG.setCompleted(prog);

    // Wrap result
    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, assignment, k);

    Clustering<KMeansModel> result = new Clustering<>();
    Metadata.of(result).setLongName("k-Means Clustering");
    for(int i = 0; i < clusters.length; i++) {
      DBIDs cids = clusters[i];
      if(cids.size() == 0) {
        continue;
      }
      result.addToplevelCluster(new Cluster<>(cids, new KMeansModel(means[i], varsum[i])));
    }
    return result;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected ParallelLloydKMeans<V> makeInstance() {
      return new ParallelLloydKMeans<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
