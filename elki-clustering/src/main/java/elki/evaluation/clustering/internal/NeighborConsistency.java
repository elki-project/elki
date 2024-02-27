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

import java.util.List;

import elki.clustering.dbscan.predicates.NearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NeighborPredicate;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.SetDBIDs;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.evaluation.Evaluator;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Average neighbor consistency: given a neighbor predict (k nearest neighbors,
 * mutual k nearest neighbors, etc.), compute what share of the points have
 * <i>all</i> their nearest neighbors in the same cluster.
 * <p>
 * TODO: noise handling for this measure?
 * <p>
 * The basic idea is found in:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 * <p>
 * Also used in the following research:
 * <p>
 * Lars Lenssen, Niklas Strahmann, Erich Schubert<br>
 * Fast k-Nearest-Neighbor-Consistent Clustering<br>
 * Lernen, Wissen, Daten, Analysen (LWDA), 2023
 *
 * @author Niklas Strahmann
 * @author Erich Schubert
 */
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
@Reference(authors = "Lars Lenssen, Niklas Strahmann, Erich Schubert", //
    title = "Fast k-Nearest-Neighbor-Consistent Clustering", //
    booktitle = "Lernen, Wissen, Daten, Analysen (LWDA)", //
    url = "https://ceur-ws.org/Vol-3630/LWDA2023-paper34.pdf", bibkey = "DBLP:conf/lwa/LenssenSS23")
public class NeighborConsistency<O> implements Evaluator {
  /**
   * Neighbor predicate
   */
  private NeighborPredicate<? super O, ?> predicate;

  /**
   * Store consistency per element.
   */
  private boolean perElement = false;

  /**
   * Constructor.
   * 
   * @param predicate Neighbor predicate
   * @param perElement Store consistency per element
   */
  public NeighborConsistency(NeighborPredicate<? super O, ?> predicate, boolean perElement) {
    super();
    this.predicate = predicate;
    this.perElement = perElement;
  }

  /**
   * Calculate (absolute) neighbor consistency for all datapoints.
   * 
   * @param clustering Cluster to evaluate
   * @param relation datapoints
   * @return fractional kNN consistency
   */
  public double evaluateClustering(Clustering<?> clustering, Relation<O> relation) {
    WritableDoubleDataStore elementKNNConsistency = perElement ? DataStoreFactory.FACTORY.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, 0.) : null;
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
        if(elementKNNConsistency != null) {
          elementKNNConsistency.put(clusterElement, 1);
        }
        consistent++;
      }
    }

    double kNNc = consistent / (double) relation.size();
    EvaluationResult ev = EvaluationResult.findOrCreate(clustering, "Clustering Evaluation");
    EvaluationResult.MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Full " + Metadata.of(predicate).getLongName() + " Consistency", kNNc, 0, 1., false);
    if(!Metadata.hierarchyOf(clustering).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    if(elementKNNConsistency != null) {
      Metadata.hierarchyOf(clustering).addChild(new MaterializedDoubleRelation("Full " + Metadata.of(predicate).getLongName() + " Consistency", relation.getDBIDs(), elementKNNConsistency));
    }
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
     * Option ID for the neighbor predicate
     */
    public static final OptionID PREDICATE_ID = new OptionID("neighbor.predicate", "Predicate to select the neighbors of each point for consistency.");

    /**
     * Option ID for per-element consistency
     */
    public static final OptionID KEEP_PER_ELEMENT_ID = new OptionID("neighbor.keep-per-element", "Keep the per-element consistency for further analysis.");

    /**
     * Neighbor predicate
     */
    private NeighborPredicate<? super O, ?> predicate;

    /**
     * Store consistency per element.
     */
    private boolean perElement = false;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NeighborPredicate<? super O, ?>>(PREDICATE_ID, NeighborPredicate.class, NearestNeighborPredicate.class) //
          .grab(config, x -> predicate = x);
      new Flag(KEEP_PER_ELEMENT_ID).grab(config, x -> perElement = x);
    }

    @Override
    public NeighborConsistency<O> make() {
      return new NeighborConsistency<>(predicate, perElement);
    }
  }
}
