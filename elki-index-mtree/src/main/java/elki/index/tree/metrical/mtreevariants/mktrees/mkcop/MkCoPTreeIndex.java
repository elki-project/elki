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
package elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import java.util.ArrayList;
import java.util.List;

import elki.database.ids.DBID;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.DistanceFunction;
import elki.index.KNNIndex;
import elki.index.RKNNIndex;
import elki.index.RangeIndex;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import elki.index.tree.metrical.mtreevariants.query.MTreeRangeQuery;
import elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import elki.persistent.PageFile;

/**
 * MkCoPTree used as database index.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public class MkCoPTreeIndex<O> extends MkCoPTree<O>implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * Relation indexed
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param pageFile Page file
   * @param settings Tree settings
   */
  public MkCoPTreeIndex(Relation<O> relation, PageFile<MkCoPTreeNode<O>> pageFile, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry> settings) {
    super(relation, pageFile, settings);
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
  protected MkCoPEntry createNewLeafEntry(DBID id, O object, double parentDistance) {
    MkCoPLeafEntry leafEntry = new MkCoPLeafEntry(id, parentDistance, null, null);
    return leafEntry;
  }

  @Override
  public void initialize() {
    super.initialize();
    List<MkCoPEntry> objs = new ArrayList<>(relation.size());
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
    return "MkCoP-Tree";
  }

  @Override
  public String getShortName() {
    return "mkcoptree";
  }
}
