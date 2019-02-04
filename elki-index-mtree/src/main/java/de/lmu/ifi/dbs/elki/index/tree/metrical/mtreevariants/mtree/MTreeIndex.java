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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.DynamicIndex;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeRangeQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;

/**
 * Class for using an m-tree as database index.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public class MTreeIndex<O> extends MTree<O>implements RangeIndex<O>, KNNIndex<O>, DynamicIndex {
  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * The distance query.
   */
  protected DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   *
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MTreeIndex(Relation<O> relation, PageFile<MTreeNode<O>> pagefile, MTreeSettings<O, MTreeNode<O>, MTreeEntry> settings) {
    super(pagefile, settings);
    this.relation = relation;
    this.distanceQuery = getDistanceFunction().instantiate(relation);
  }

  @Override
  public double distance(DBIDRef id1, DBIDRef id2) {
    if(id1 == null || id2 == null) {
      return Double.NaN;
    }
    if(DBIDUtil.equal(id1, id2)) {
      return 0.;
    }
    statistics.countDistanceCalculation();
    return distanceQuery.distance(id1, id2);
  }

  @Override
  protected void initializeCapacities(MTreeEntry exampleLeaf) {
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE; // exampleLeaf.getParentDistance().externalizableSize();

    // FIXME: simulate a proper feature size!
    @SuppressWarnings("unchecked")
    Relation<? extends SpatialComparable> vrel = (Relation<? extends SpatialComparable>) relation;
    final int dim = RelationUtil.dimensionality(vrel);
    int featuresize = 8 * dim;
    if(dim <= 0) {
      getLogger().warning("Relation does not have a dimensionality -- simulating M-tree as external index!");
      featuresize = 0;
    }

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID + coveringRadius
    // + parentDistance) + 1
    // dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize +
    // distanceSize) + 1;

    // dirCapacity = (pageSize - overhead) / (nodeID + **object feature size** +
    // coveringRadius + parentDistance) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + featuresize + distanceSize + distanceSize) + 1;

    if(dirCapacity <= 2) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      getLogger().warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }
    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance) + 1
    // leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize) + 1;
    // leafCapacity = (pageSize - overhead) / (objectID + ** object size ** +
    // parentDistance) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + featuresize + distanceSize) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      getLogger().warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }
  }

  /**
   * @return a new MTreeLeafEntry representing the specified data object
   */
  protected MTreeEntry createNewLeafEntry(DBID id, O object, double parentDistance) {
    return new MTreeLeafEntry(id, parentDistance);
  }

  @Override
  public void initialize() {
    super.initialize();
    insertAll(relation.getDBIDs());
  }

  @Override
  public void insert(DBIDRef id) {
    insert(createNewLeafEntry(DBIDUtil.deref(id), relation.get(id), Double.NaN), false);
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MTreeEntry> objs = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, Double.NaN));
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
    throw new NotImplementedException();
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
    throw new NotImplementedException();
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
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    return new MTreeKNNQuery<>(this, dq);
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
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    return new MTreeRangeQuery<>(this, dq);
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
