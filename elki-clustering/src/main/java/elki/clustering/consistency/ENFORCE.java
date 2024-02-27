/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.consistency;

import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NeighborPredicate;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * ENFORCE: clustering, then enforcing neighborhood consistency
 * <p>
 * Reference:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 * 
 * @author Niklas Strahmann
 *
 * @param <O> Input data type
 */
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
public class ENFORCE<O> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ENFORCE.class);

  /**
   * Base clustering algorithm.
   */
  private final ClusteringAlgorithm<?> baseAlgorithm;

  /**
   * Neighbor predicate
   */
  NeighborPredicate<? super O, ?> predicate;

  /**
   * Constructor.
   *
   * @param baseAlgorithm Base algorithm
   * @param predicate Neighbor predicate
   */
  public ENFORCE(ClusteringAlgorithm<?> baseAlgorithm, NeighborPredicate<? super O, ?> predicate) {
    this.baseAlgorithm = baseAlgorithm;
    this.predicate = predicate;
  }

  /**
   * Run the clustering algorithm.
   * 
   * @param db Database
   * @return Clustering result
   */
  public Clustering<Model> run(Database db) {
    Clustering<?> baseResult = baseAlgorithm.autorun(db);

    Relation<O> relation = db.getRelation(predicate.getInputTypeRestriction());
    List<DBIDs> closedNeighborhoods = ClosedNeighborhoods.getClosedNeighborhoods(predicate, relation, LOG, this.getClass().getName());

    int clusterAmount = baseResult.getAllClusters().size();
    ModifiableDBIDs[] finalCluster = new ModifiableDBIDs[clusterAmount];
    for(int i = 0; i < clusterAmount; i++) {
      finalCluster[i] = DBIDUtil.newArray();
    }

    for(DBIDs closedNeighborhood : closedNeighborhoods) {
      int[] clusterCounter = new int[clusterAmount];
      int clusterIndex = 0;
      for(It<? extends Cluster<?>> cluster = baseResult.iterToplevelClusters(); cluster.valid(); cluster.advance()) {
        DBIDs clusterDBIDs = cluster.get().getIDs();
        for(DBIDIter cnsElement = closedNeighborhood.iter(); cnsElement.valid(); cnsElement.advance()) {
          if(clusterDBIDs.contains(cnsElement)) {
            clusterCounter[clusterIndex]++;
          }
        }
        clusterIndex++;
      }
      finalCluster[VMath.argmax(clusterCounter)].addDBIDs(closedNeighborhood);
    }

    Clustering<Model> clustering = new Clustering<>();
    for(int i = 0; i < clusterAmount; i++) {
      if(!finalCluster[i].isEmpty()) {
        clustering.addToplevelCluster(new Cluster<>(finalCluster[i]));
      }
    }
    return clustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return new TypeInformation[0];
  }

  /**
   * Parameterizer.
   * 
   * @author Niklas Strahmann
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Option ID for parameterization
     */
    public static final OptionID PREDICATE_ID = ClosedNeighborhoods.Par.PREDICATE_ID;

    /**
     * Underlying base clustering algorithm.
     */
    protected ClusteringAlgorithm<?> baseAlgorithm;

    /**
     * Neighbor predicate
     */
    NeighborPredicate<? super O, ?> predicate;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<ClusteringAlgorithm<?>>(Utils.ALGORITHM_ID, ClusteringAlgorithm.class) //
          .grab(config, x -> baseAlgorithm = x);
      new ObjectParameter<NeighborPredicate<? super O, ?>>(PREDICATE_ID, NeighborPredicate.class, MutualNearestNeighborPredicate.class) //
          .grab(config, (p) -> predicate = p);
    }

    @Override
    public ENFORCE<O> make() {
      return new ENFORCE<>(baseAlgorithm, predicate);
    }
  }
}
