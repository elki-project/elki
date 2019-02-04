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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeRangeQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * MkTabTree used as database index.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public class MkTabTreeIndex<O> extends MkTabTree<O>implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MkTabTreeIndex(Relation<O> relation, PageFile<MkTabTreeNode<O>> pagefile, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry> settings) {
    super(relation, pagefile, settings);
    this.relation = relation;
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   */
  protected MkTabEntry createNewLeafEntry(DBID id, O object, double parentDistance) {
    return new MkTabLeafEntry(id, parentDistance, knnDistances(object));
  }

  /**
   * Returns the knn distance of the object with the specified id.
   * 
   * @param object the query object
   * @return the knn distance of the object with the specified id
   */
  private double[] knnDistances(O object) {
    KNNList knns = knnq.getKNNForObject(object, getKmax() - 1);
    double[] distances = new double[getKmax()];
    int i = 0;
    for(DoubleDBIDListIter iter = knns.iter(); iter.valid() && i < getKmax(); iter.advance(), i++) {
      distances[i] = iter.doubleValue();
    }
    return distances;
  }

  @Override
  public void initialize() {
    super.initialize();
    List<MkTabEntry> objs = new ArrayList<>(relation.size());
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter); // FIXME: expensive
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, Double.NaN));
    }
    insertAll(objs);
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MTreeKNNQuery<>(this, distanceQuery);
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MTreeRangeQuery<>(this, distanceQuery);
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new MkTreeRKNNQuery<>(this, distanceQuery);
  }

  @Override
  public String getLongName() {
    return "MkTab-Tree";
  }

  @Override
  public String getShortName() {
    return "mktabtree";
  }
}
