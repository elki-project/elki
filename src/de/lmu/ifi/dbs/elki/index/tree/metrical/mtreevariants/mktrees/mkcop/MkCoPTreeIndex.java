package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeQueryUtil;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * MkCoPTree used as database index.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkCoPTreeIndex<O, D extends NumberDistance<D, ?>> extends MkCoPTree<O, D> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
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
  public MkCoPTreeIndex(Relation<O> relation, PageFile<MkCoPTreeNode<O, D>> pageFile, MkTreeSettings<O, D, MkCoPTreeNode<O, D>, MkCoPEntry> settings) {
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
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter); // FIXME: expensive
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, Double.NaN));
    }
    insertAll(objs);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, D> distanceFunction = (DistanceFunction<? super O, D>) distanceQuery.getDistanceFunction();
    if (!this.getDistanceFunction().equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    DistanceQuery<O, D> dq = distanceFunction.instantiate(relation);
    return (KNNQuery<O, S>) MTreeQueryUtil.getKNNQuery(this, dq, hints);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RangeQuery<O, S> getRangeQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, D> distanceFunction = (DistanceFunction<? super O, D>) distanceQuery.getDistanceFunction();
    if (!this.getDistanceFunction().equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    DistanceQuery<O, D> dq = distanceFunction.instantiate(relation);
    return (RangeQuery<O, S>) MTreeQueryUtil.getRangeQuery(this, dq, hints);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    DistanceFunction<? super O, D> distanceFunction = (DistanceFunction<? super O, D>) distanceQuery.getDistanceFunction();
    if (!this.getDistanceFunction().equals(distanceFunction)) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    DistanceQuery<O, D> dq = distanceFunction.instantiate(relation);
    return (RKNNQuery<O, S>) new MkTreeRKNNQuery<>(this, dq);
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
