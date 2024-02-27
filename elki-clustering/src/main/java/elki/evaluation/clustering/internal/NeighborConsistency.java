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
package elki.evaluation.clustering.internal;

import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NeighborPredicate;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.evaluation.Evaluator;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import java.util.List;

/**
 * Average neighbor consistency: given a neighbor predict (mutual nearest
 * neighbors, nearest neighbors, etc.), compute what share of the points have
 * <i>all</i> their nearest neighbors in the same cluster.
 * 
 * @author Niklas Strahmann
 * @author Erich Schubert
 */
public class NeighborConsistency<O> implements Evaluator {
  /**
   * Neighbor predicate
   */
  private NeighborPredicate<? super O, ?> predicate;

  /**
   * Constructor.
   * 
   * @param predicate Neighbor predicate
   */
  public NeighborConsistency(NeighborPredicate<? super O, ?> predicate) {
    super();
    this.predicate = predicate;
  }

  /**
   * Calculate (absolute) neighbor consistency for all datapoints.
   * 
   * @param clustering Cluster to evaluate
   * @param relation datapoints
   * @return fractional kNN consistency
   */
  public double evaluateClustering(Clustering<?> clustering, Relation<O> relation) {
    WritableDoubleDataStore elementKNNConsistency = DataStoreFactory.FACTORY.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, 0.);
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    @SuppressWarnings("unchecked") // We do not need the inner data type
    NeighborPredicate.Instance<Object> pred = (NeighborPredicate.Instance<Object>) predicate.instantiate(relation);

    int consistent = 0;
    for(Cluster<?> cluster : clusters) {
      SetDBIDs clusterIDs = DBIDUtil.ensureSet(cluster.getIDs());
      elementLoop: for(DBIDIter clusterElement = clusterIDs.iter(); clusterElement.valid(); clusterElement.advance()) {
        for(DBIDIter neighbor = pred.iterDBIDs(pred.getNeighbors(clusterElement)); neighbor.valid(); neighbor.advance()) {
          if(!DBIDUtil.equal(clusterElement, neighbor) && !clusterIDs.contains(neighbor)) {
            continue elementLoop;
          }
        }
        elementKNNConsistency.put(clusterElement, 1);
        consistent++;
      }
    }

    double kNNc = consistent / (double) relation.size();

    EvaluationResult ev = EvaluationResult.findOrCreate(clustering, "Clustering Evaluation");
    EvaluationResult.MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    // FIXME: use better distinguishable names, include k
    g.addMeasure("Absolute Neighbor Consistency", kNNc, 0, 1., false);
    if(!Metadata.hierarchyOf(clustering).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    // TODO: make optional.
    Metadata.hierarchyOf(clustering).addChild(new MaterializedDoubleRelation("Absolute Neighbor Consistency", relation.getDBIDs(), elementKNNConsistency));
    return kNNc;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> clusters = Clustering.getClusteringResults(result);
    if(clusters.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<O> relation = db.getRelation(predicate.getInputTypeRestriction());
    for(Clustering<?> cluster : clusters) {
      evaluateClustering(cluster, relation);
    }
  }

  /**
   * Parameterization class
   * 
   * @author Niklas Strahmann
   * @author Erich Schubert
   * 
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Option ID for parameterization
     */
    public static final OptionID PREDICATE_ID = new OptionID("neighbor.predicate", "Predicate to select the neighbors of each point for consistency.");

    /**
     * Neighbor predicate
     */
    private NeighborPredicate<? super O, ?> predicate;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NeighborPredicate<? super O, ?>>(PREDICATE_ID, NeighborPredicate.class, MutualNearestNeighborPredicate.class) //
          .grab(config, x -> predicate = x);
    }

    @Override
    public NeighborConsistency<O> make() {
      return new NeighborConsistency<>(predicate);
    }
  }
}
