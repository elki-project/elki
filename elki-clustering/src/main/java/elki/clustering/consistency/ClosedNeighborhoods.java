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

import java.util.ArrayList;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NeighborPredicate;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.ids.HashSetModifiableDBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.ids.StaticDBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.utilities.datastructures.unionfind.UnionFind;
import elki.utilities.datastructures.unionfind.UnionFindUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Pseudo-clustering algorithm that treats each closed neighbor set as a
 * cluster. This is related to DBSCAN clustering, which uses similar closures.
 * 
 * @author Niklas Strahmann
 * 
 * @param <O> Object type
 */
public class ClosedNeighborhoods<O> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClosedNeighborhoods.class);

  /**
   * Neighbor predicate
   */
  NeighborPredicate<? super O, ?> predicate;

  /**
   * Constructor.
   * 
   * @param predicate Neighbor predicate
   */
  public ClosedNeighborhoods(NeighborPredicate<? super O, ?> predicate) {
    this.predicate = predicate;
  }

  /**
   * Run the clustering algorithm.
   * 
   * @param relation Data relation
   * @return Clustering result
   */
  public Clustering<Model> run(Relation<? extends O> relation) {
    List<DBIDs> CNSs = getClosedNeighborhoods(predicate, relation, LOG, this.getClass().getName());
    Clustering<Model> clustering = new Clustering<>();
    for(DBIDs cns : CNSs) {
      clustering.addToplevelCluster(new Cluster<>(cns));
    }
    return clustering;
  }

  /**
   * Get the closed neighborhoods
   * 
   * @param <O> Object type
   * @param predicate Neighborhood predicate
   * @param relation Data relation
   * @param log Logger
   * @param key Measurement prefix
   * @return Neighborhood sets
   */
  public static <O> List<DBIDs> getClosedNeighborhoods(NeighborPredicate<? super O, ?> predicate, Relation<? extends O> relation, Logging log, String key) {
    @SuppressWarnings("unchecked") // We do not need to know the exact type
    NeighborPredicate.Instance<Object> inst = (NeighborPredicate.Instance<Object>) predicate.instantiate(relation);
    Duration cnsTime = log.isStatistics() ? log.newDuration(key + ".neighborhoods.time").begin() : null;
    StaticDBIDs ids = DBIDUtil.makeUnmodifiable(relation.getDBIDs());
    HashSetModifiableDBIDs visited = DBIDUtil.newHashSet(ids.size());
    ModifiableDBIDs open = DBIDUtil.newArray();
    DBIDVar cur = DBIDUtil.newVar();
    UnionFind uf = UnionFindUtil.make(ids);
    for(DBIDIter element = relation.iterDBIDs(); element.valid(); element.advance()) {
      if(!visited.add(element)) {
        continue;
      }
      assert open.isEmpty();
      open.add(element);
      while(!open.isEmpty()) {
        open.pop(cur);
        for(DBIDIter iter = inst.iterDBIDs(inst.getNeighbors(cur)); iter.valid(); iter.advance()) {
          uf.union(element, iter);
          if(visited.add(iter)) {
            open.add(iter);
          }
        }
      }
    }
    // Map union-find to DBIDs
    Int2ObjectMap<ModifiableDBIDs> tmp = new Int2ObjectOpenHashMap<>();
    List<DBIDs> connectedComponents = new ArrayList<>();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      int i = uf.find(iter);
      ModifiableDBIDs comp = tmp.get(i);
      if (comp == null) {
        tmp.put(i, comp = DBIDUtil.newArray(uf.size(i)));
        connectedComponents.add(comp);
      }
      comp.add(iter);
    }
    if(log.isStatistics()) {
      log.statistics(cnsTime.end());
      log.statistics(new LongStatistic(key + ".neighborhoods", connectedComponents.size()));
      log.statistics(new DoubleStatistic(key + ".neighborhoods.average-size", relation.size() / (double) connectedComponents.size()));
    }
    return (List<DBIDs>) connectedComponents;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(predicate.getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Niklas Strahmann
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
    NeighborPredicate<? super O, ?> predicate;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NeighborPredicate<? super O, ?>>(PREDICATE_ID, NeighborPredicate.class, MutualNearestNeighborPredicate.class) //
          .grab(config, (p) -> predicate = p);
    }

    @Override
    public ClosedNeighborhoods<O> make() {
      return new ClosedNeighborhoods<>(predicate);
    }
  }
}
