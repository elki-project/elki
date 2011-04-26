package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MTreeNode oneway - - contains
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 */
@Title("M-Tree")
@Description("Efficient Access Method for Similarity Search in Metric Spaces")
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", url = "http://www.vldb.org/conf/1997/P426.PDF")
public class MTree<O, D extends Distance<D>> extends AbstractMTree<O, D, MTreeNode<O, D>, MTreeEntry<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MTree.class);

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public MTree(Relation<O> relation, String fileName, int pageSize, long cacheSize, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(relation, fileName, pageSize, cacheSize, distanceQuery, distanceFunction);
  }

  /**
   * Inserts the specified object into this M-Tree by calling
   * {@link AbstractMTree#insert
   * AbstractMTree.insert(id, object, false)}.
   * 
   * @param object the object to be inserted
   */
  @Override
  public void insert(DBID id) {
    this.insert(id, relation.get(id), false);
  }

  /**
   * Inserts the specified objects into this M-Tree sequentially since a bulk
   * load method is not implemented so far. Calls for each object
   * {@link AbstractMTree#insert
   * AbstractMTree.insert(id, object, false)}.
   */
  // todo: bulk load method
  @Override
  public void insertAll(DBIDs ids) {
    if (ids.isEmpty()) {
      return;
    }
    for (DBID id : ids) {
      insert(id, relation.get(id), false);
    }

    if(extraIntegrityChecks) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * Does nothing because no operations are necessary before inserting an entry.
   */
  @Override
  protected void preInsert(@SuppressWarnings("unused") MTreeEntry<D> entry) {
    // do nothing
  }

  @Override
  protected void initializeCapacities(MTreeEntry<D> exampleLeaf) {
    int distanceSize = exampleLeaf.getParentDistance().externalizableSize();

    // FIXME: simulate a proper feature size!
    int featuresize = 0; //DatabaseUtil.dimensionality(relation);

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance) + 1
    // dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize +
    // distanceSize) + 1;

    // dirCapacity = (pageSize - overhead) / (nodeID + **object feature size** +
    // coveringRadius + parentDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + featuresize + distanceSize + distanceSize) + 1;

    if(dirCapacity <= 2) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }
    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance) +
    // 1
    // leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize) + 1;
    // leafCapacity = (pageSize - overhead) / (objectID + ** object size ** +
    // parentDistance) +
    // 1
    leafCapacity = (int) (pageSize - overhead) / (4 + featuresize + distanceSize) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    if(logger.isVerbose()) {
      logger.verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
    }
  }

  /**
   * @return a new MTreeLeafEntry representing the specified data object
   */
  @Override
  protected MTreeEntry<D> createNewLeafEntry(DBID id, O object, D parentDistance) {
    return new MTreeLeafEntry<D>(id, parentDistance);
  }

  /**
   * @return a new MTreeDirectoryEntry representing the specified node
   */
  @Override
  protected MTreeEntry<D> createNewDirectoryEntry(MTreeNode<O, D> node, DBID routingObjectID, D parentDistance) {
    return new MTreeDirectoryEntry<D>(routingObjectID, parentDistance, node.getPageID(), node.coveringRadius(routingObjectID, this));
  }

  /**
   * @return a new MTreeDirectoryEntry by calling
   *         <code>new MTreeDirectoryEntry<D>(null, null, 0, null)</code>
   */
  @Override
  protected MTreeEntry<D> createRootEntry() {
    return new MTreeDirectoryEntry<D>(null, null, 0, null);
  }

  /**
   * @return a new MTreeNode which is a leaf node
   */
  @Override
  protected MTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, true);
  }

  /**
   * @return a new MTreeNode which is a directory node
   */
  @Override
  protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<MTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MTreeNode.class);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}