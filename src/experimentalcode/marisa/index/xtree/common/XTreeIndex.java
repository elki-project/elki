package experimentalcode.marisa.index.xtree.common;

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

public class XTreeIndex<O extends NumberVector<?, ?>> extends XTree implements RangeIndex<O>, KNNIndex<O> {
  private static final Logging logger = Logging.getLogger(XTreeIndex.class);

  private Relation<O> relation;

  public XTreeIndex(Relation<O> relation, PageFile<XTreeNode> pagefile, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, double relativeMinEntries, double relativeMinFanout, float reinsert_fraction, float max_overlap, int overlap_type) {
    super(pagefile, bulkSplitter, insertionStrategy, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
    this.relation = relation;
    this.initialize();
  }

  protected SpatialEntry createNewLeafEntry(DBID id) {
    return new SpatialPointLeafEntry(id, relation.get(id));
  }

  /**
   * Inserts the specified real vector object into this index.
   * 
   * @param id the object id that was inserted
   */
  @Override
  public final void insert(DBID id) {
    insertLeaf(createNewLeafEntry(id));
  }

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param objects the objects to be inserted
   */
  @Override
  public final void insertAll(DBIDs ids) {
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
  public final boolean delete(DBID id) {
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
    return "X-Tree";
  }

  @Override
  public String getShortName() {
    return "xtree";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}