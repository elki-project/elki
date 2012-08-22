package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeQueryUtil;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Class for using an m-tree as database index.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeIndex<O, D extends Distance<D>> extends MTree<O, D> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   *
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public MTreeIndex(Relation<O> relation, PageFile<MTreeNode<O, D>> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(pagefile, distanceQuery, distanceFunction);
    this.relation = relation;
    this.initialize();
  }

  /**
   * @return a new MTreeLeafEntry representing the specified data object
   */
  protected MTreeEntry<D> createNewLeafEntry(DBID id, O object, D parentDistance) {
    return new MTreeLeafEntry<D>(id, parentDistance);
  }

  @Override
  public void insert(DBIDRef id) {
    insert(createNewLeafEntry(DBIDUtil.deref(id), relation.get(id), getDistanceFactory().undefinedDistance()), false);
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MTreeEntry<D>> objs = new ArrayList<MTreeEntry<D>>(ids.size());
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, getDistanceFactory().undefinedDistance()));
    }
    insertAll(objs);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @Override
  public final boolean delete(DBIDRef id) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @Override
  public void deleteAll(DBIDs ids) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return MTreeQueryUtil.getKNNQuery(idx, dq, hints);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RangeQuery<O, S> getRangeQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return MTreeQueryUtil.getRangeQuery(idx, dq);
  }

  @Override
  public String getLongName() {
    return "M-Tree";
  }

  @Override
  public String getShortName() {
    return "mtree";
  }
}