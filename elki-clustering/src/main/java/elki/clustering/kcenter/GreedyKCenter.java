/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.kcenter;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.ClusteringAlgorithmUtil;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.SimplePrototypeModel;
import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Greedy algorithm for k-center algorithm also known as Gonzalez clustering,
 * or farthest-first traversal.
 * <p>
 * The first cluster center is chosen arbitrarily. The remaining are always
 * chosen such as to maximize the distance to their current cluster.
 * <p>
 * Reference:
 * <p>
 * T. F. Gonzalez:<br>
 * Clustering to Minimize the Maximum Intercluster Distance<br>
 * Theoretical Computer Science, 38, 1985
 * <p>
 * D. S. Hochbaum, D. B. Shmoys<br>
 * A unified approach to approximation algorithms for bottleneck problems<br>
 * Journal of the ACM, 33 (3), 1986
 * 
 * @author Robert Gehde
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type to cluster
 */
@Title("Greedy K-center Clustering")
@Reference(authors = "T. F. Gonzalez", //
    title = "Clustering to Minimize the Maximum Intercluster Distance", //
    booktitle = "Theoretical Computer Science, 38", //
    url = "https://doi.org/10.1016/0304-3975(85)90224-5", //
    bibkey = "DBLP:journals/tcs/Gonzalez85")
@Reference(authors = "D. S. Hochbaum, D. B. Shmoys", //
    title = "A unified approach to approximation algorithms for bottleneck problems", //
    booktitle = "Journal of the ACM, 33 (3), 1986", //
    url = "https://doi.org/10.1145/5925.5933", //
    bibkey = "DBLP:journals/jacm/HochbaumS86")
public class GreedyKCenter<O> implements ClusteringAlgorithm<Clustering<SimplePrototypeModel<O>>> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(GreedyKCenter.class);

  /**
   * Distance function
   */
  Distance<? super O> distance;

  /**
   * number of clusters
   */
  int k;

  /**
   * Random factory for choosing the first element.
   */
  RandomFactory rand;

  /**
   * 
   * Constructor.
   *
   * @param k number of clusters
   * @param distance distance function to use
   * @param rand random factory
   */
  public GreedyKCenter(int k, Distance<? super O> distance, RandomFactory rand) {
    this.k = k;
    this.distance = distance;
    this.rand = rand;
  }

  /**
   * Perform greedy k-center clustering on the relation.
   * 
   * @param relation data to cluster
   * @return clustering
   */
  public Clustering<SimplePrototypeModel<O>> run(Relation<O> relation) {
    if(relation.size() == 0) {
      // we directly access the first element, so having at least should be nice
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    final DBIDs ids = relation.getDBIDs();
    FiniteProgress progCluster = LOG.isVerbose() ? new FiniteProgress("Greedy k-center number of clusters", k, LOG) : null;
    FiniteProgress progDist = LOG.isVerbose() ? new FiniteProgress("Greedy k-center distance computations", k * ids.size(), LOG) : null;

    DistanceQuery<O> distq = distance.instantiate(relation);
    WritableDoubleDataStore distanceToHead = DataStoreFactory.FACTORY.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    WritableIntegerDataStore clusAssignments = DataStoreFactory.FACTORY.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0);
    ArrayModifiableDBIDs heads = DBIDUtil.newArray(k);

    DBIDVar curHead = DBIDUtil.newVar();
    DBIDVar nextHead = DBIDUtil.randomSample(ids, rand);
    for(int i = 0; i < k; i++) {
      curHead.set(nextHead);
      double maxd = 0;
      clusAssignments.putInt(curHead, i);
      heads.add(curHead);

      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        // calc distance to Head
        final double d = distq.distance(curHead, it);
        double od = distanceToHead.doubleValue(it);
        // if close to new head, assign to new head
        if(d < od) {
          distanceToHead.putDouble(it, od = d);
          clusAssignments.putInt(it, i);
        }
        // find furthest for next iteration
        if(maxd < od) {
          maxd = od;
          nextHead.set(it);
        }
        LOG.incrementProcessed(progDist);
      }
      LOG.incrementProcessed(progCluster);
    }
    LOG.ensureCompleted(progDist);
    LOG.ensureCompleted(progCluster);
    // final cluster assignment
    Clustering<SimplePrototypeModel<O>> result = new Clustering<SimplePrototypeModel<O>>();
    ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, clusAssignments, k);
    for(DBIDArrayIter headsit = heads.iter(); headsit.valid(); headsit.advance()) {
      result.addToplevelCluster(new Cluster<SimplePrototypeModel<O>>(clusters[headsit.getOffset()], //
          new SimplePrototypeModel<O>(relation.get(headsit))));
    }
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return new TypeInformation[] { distance.getInputTypeRestriction() };
  }

  /**
   * Parameterization class
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   *
   * @param <O> object type to cluster
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the number of clusters
     */
    public static final OptionID K_ID = new OptionID("greedy.k", "Number of centers to choose.");

    /**
     * Parameter for the random seed
     */
    public static final OptionID RANDOM_ID = new OptionID("greedy.seed", "Random seed to use for choosing the first.");

    /**
     * Distance function to use
     */
    Distance<? super O> distance;

    /**
     * number of clusters
     */
    int k;

    /**
     * Random factory for choosing the first element.
     */
    RandomFactory rand;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
          .grab(config, x -> this.k = x);
      new ObjectParameter<Distance<O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> this.distance = x);
      new RandomParameter(RANDOM_ID) //
          .grab(config, x -> this.rand = x);
    }

    @Override
    public GreedyKCenter<O> make() {
      return new GreedyKCenter<>(k, distance, rand);
    }
  }
}
