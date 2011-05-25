package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

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
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * The common use of the DeLiClu tree: indexing number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class DeLiCluTreeIndex<O extends NumberVector<?, ?>> extends DeLiCluTree implements KNNIndex<O>, RangeIndex<O> {
  /**
   * The relation we index
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param pagefile Page file
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   * @param insertionCandidates insertion candidate set size
   */
  public DeLiCluTreeIndex(Relation<O> relation, PageFile<DeLiCluNode> pagefile, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(pagefile, bulk, bulkLoadStrategy, insertionCandidates);
    this.relation = relation;
    this.initialize();
  }

  /**
   * The appropriate logger for this index.
   */
  private static final Logging logger = Logging.getLogger(DeLiCluTreeIndex.class);

  /**
   * Creates a new leaf entry representing the specified data object.
   * 
   * @param id Object id
   */
  protected DeLiCluLeafEntry createNewLeafEntry(DBID id) {
    return new DeLiCluLeafEntry(id, relation.get(id));
  }

  /**
   * Marks the specified object as handled and returns the path of node ids from
   * the root to the objects's parent.
   * 
   * @param o the object to be marked as handled
   * @return the path of node ids from the root to the objects's parent
   */
  public synchronized List<TreeIndexPathComponent<DeLiCluEntry>> setHandled(DBID id, O obj) {
    if(logger.isDebugging()) {
      logger.debugFine("setHandled " + id + ", " + obj + "\n");
    }

    // find the leaf node containing o
    IndexTreePath<DeLiCluEntry> pathToObject = findPathToObject(getRootPath(), obj, id);

    if(pathToObject == null) {
      return null;
    }

    // set o handled
    DeLiCluEntry entry = pathToObject.getLastPathComponent().getEntry();
    entry.setHasHandled(true);
    entry.setHasUnhandled(false);

    for(IndexTreePath<DeLiCluEntry> path = pathToObject; path.getParentPath() != null; path = path.getParentPath()) {
      DeLiCluEntry parentEntry = path.getParentPath().getLastPathComponent().getEntry();
      DeLiCluNode node = getNode(parentEntry);
      boolean hasHandled = false;
      boolean hasUnhandled = false;
      for(int i = 0; i < node.getNumEntries(); i++) {
        final DeLiCluEntry nodeEntry = node.getEntry(i);
        hasHandled = hasHandled || nodeEntry.hasHandled();
        hasUnhandled = hasUnhandled || nodeEntry.hasUnhandled();
      }
      parentEntry.setHasUnhandled(hasUnhandled);
      parentEntry.setHasHandled(hasHandled);
    }

    return pathToObject.getPath();
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
      List<DeLiCluEntry> leafs = new ArrayList<DeLiCluEntry>(ids.size());
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
    IndexTreePath<DeLiCluEntry> deletionPath = findPathToObject(getRootPath(), obj, id);
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
    return "DeLiClu-Tree";
  }

  @Override
  public String getShortName() {
    return "deliclutree";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}