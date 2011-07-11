package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.InsertionStrategy;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * The common use of the rstar tree: indexing number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class RStarTreeIndex<O extends NumberVector<?, ?>> extends RStarTree implements RangeIndex<O>, KNNIndex<O> {
  /**
   * The appropriate logger for this index.
   */
  private static final Logging logger = Logging.getLogger(RStarTreeIndex.class);

  /**
   * Relation
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param pagefile Page file
   * @param bulkSplitter bulk load strategy
   * @param insertionStrategy the strategy to find the insertion child
   */
  public RStarTreeIndex(Relation<O> relation, PageFile<RStarTreeNode> pagefile, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy) {
    super(pagefile, bulkSplitter, insertionStrategy);
    this.relation = relation;
    this.initialize();
  }

  /**
   * Create a new leaf entry.
   * 
   * @param id Object id
   * @return Spatial leaf entry
   */
  protected SpatialPointLeafEntry createNewLeafEntry(DBID id) {
    return new SpatialPointLeafEntry(id, relation.get(id));
  }

  /**
   * Inserts the specified reel vector object into this index.
   * 
   * @param id the object id that was inserted
   */
  @Override
  public void insert(DBID id) {
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
    if(ids.isEmpty() || (ids.size() == 1)) {
      return;
    }

    // Make an example leaf
    if(canBulkLoad()) {
      List<SpatialEntry> leafs = new ArrayList<SpatialEntry>(ids.size());
      for(DBID id : ids) {
        leafs.add(createNewLeafEntry(id));
      }
      bulkLoad(leafs);
    }
    else {
      for(DBID id : ids) {
        insert(id);
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
  public boolean delete(DBID id) {
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
    for(DBID id : ids) {
      delete(id);
    }
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    SpatialDistanceQuery<O, D> dq = (SpatialDistanceQuery<O, D>) distanceQuery;
    return RStarTreeUtil.getRangeQuery(this, dq, hints);
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    SpatialDistanceQuery<O, D> dq = (SpatialDistanceQuery<O, D>) distanceQuery;
    return RStarTreeUtil.getKNNQuery(this, dq, hints);
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
    return logger;
  }
}