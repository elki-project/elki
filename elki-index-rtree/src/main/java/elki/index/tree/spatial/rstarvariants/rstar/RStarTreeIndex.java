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
package elki.index.tree.spatial.rstarvariants.rstar;

import java.util.ArrayList;
import java.util.List;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.distance.SpatialDistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.index.DistancePriorityIndex;
import elki.index.DynamicIndex;
import elki.index.tree.IndexTreePath;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.SpatialPointLeafEntry;
import elki.index.tree.spatial.rstarvariants.RTreeSettings;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil;
import elki.logging.Logging;
import elki.persistent.PageFile;

/**
 * The common use of the rstar tree: indexing number vectors.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public class RStarTreeIndex<O extends NumberVector> extends RStarTree implements DistancePriorityIndex<O>, DynamicIndex {
  /**
   * The appropriate logger for this index.
   */
  private static final Logging LOG = Logging.getLogger(RStarTreeIndex.class);

  /**
   * Relation
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public RStarTreeIndex(Relation<O> relation, PageFile<RStarTreeNode> pagefile, RTreeSettings settings) {
    super(pagefile, settings);
    this.relation = relation;
  }

  /**
   * Create a new leaf entry.
   * 
   * @param id Object id
   * @return Spatial leaf entry
   */
  protected SpatialPointLeafEntry createNewLeafEntry(DBIDRef id) {
    return new SpatialPointLeafEntry(DBIDUtil.deref(id), relation.get(id));
  }

  @Override
  public void initialize() {
    super.initialize();
    insertAll(relation.getDBIDs()); // Will check for actual bulk load!
  }

  /**
   * Inserts the specified reel vector object into this index.
   * 
   * @param id the object id that was inserted
   */
  @Override
  public void insert(DBIDRef id) {
    insertLeaf(createNewLeafEntry(id));
  }

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param ids the objects to be inserted
   */
  @Override
  public void insertAll(DBIDs ids) {
    if(ids.isEmpty()) {
      return;
    }

    // Make an example leaf
    if(canBulkLoad()) {
      List<SpatialEntry> leafs = new ArrayList<>(ids.size());
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        leafs.add(createNewLeafEntry(iter));
      }
      bulkLoad(leafs);
    }
    else {
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        insert(DBIDUtil.deref(iter));
      }
    }

    doExtraIntegrityChecks();
  }

  /**
   * Deletes the specified object from this index.
   * 
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   */
  @Override
  public boolean delete(DBIDRef id) {
    // find the leaf node containing o
    O obj = relation.get(id);
    IndexTreePath<SpatialEntry> deletionPath = findPathToObject(getRootPath(), obj, id);
    if(deletionPath == null) {
      return false;
    }
    deletePath(deletionPath);
    return true;
  }

  @Override
  public void deleteAll(DBIDs ids) {
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      delete(iter);
    }
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    // Can we support this distance function - spatial distances only!
    return distanceQuery.getRelation() == relation && distanceQuery instanceof SpatialDistanceQuery ? //
        RStarTreeUtil.getKNNQuery(this, (SpatialDistanceQuery<O>) distanceQuery, maxk, flags) : null;
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    // Can we support this distance function - spatial distances only!
    return distanceQuery.getRelation() == relation && distanceQuery instanceof SpatialDistanceQuery ? //
        RStarTreeUtil.getRangeQuery(this, (SpatialDistanceQuery<O>) distanceQuery, maxradius, flags) : null;
  }

  @Override
  public DistancePrioritySearcher<O> getPriorityQuery(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    // Can we support this distance function - spatial distances only!
    return distanceQuery.getRelation() == relation && distanceQuery instanceof SpatialDistanceQuery ? //
        RStarTreeUtil.getDistancePrioritySearcher(this, (SpatialDistanceQuery<O>) distanceQuery, maxradius, flags) : null;
  }

  @Override
  public String getLongName() {
    return "R*-Tree";
  }

  @Override
  public String getShortName() {
    return "rstartree";
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
