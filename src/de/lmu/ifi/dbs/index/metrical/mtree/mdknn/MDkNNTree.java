package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.MTree;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MDkNNTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MDkNNTree<O extends MetricalObject, D extends Distance> extends MTree<O, D> {

  private int k;

  /**
   * Creates a new MDkNNTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MTree
   * @param cacheSize the size of the cache in bytes
   */
  public MDkNNTree(String fileName, int cacheSize) {
    init(new MDkNNTreeHeader(), fileName, cacheSize);
  }

  /**
   * Creates a new MDkNNTree with the specified parameters.
   * The MTree will be hold in main memory.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   * @param k                the parameter k
   */
  public MDkNNTree(String fileName, int pageSize, int cacheSize,
                   DistanceFunction<O, D> distanceFunction, int k) {
    super();
    this.k = k;
    init(fileName, pageSize, cacheSize, distanceFunction);
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    MDkNNLeafEntry<D> newEntry = (MDkNNLeafEntry<D>) doInsert(object);

    KNNList<D> knns_newEntry = new KNNList<D>(k, distanceFunction.infiniteDistance());
    MDkNNTreeNode<O, D> root = (MDkNNTreeNode<O, D>) getRoot();
    postInsert(newEntry, null, root, knns_newEntry);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
    if (k != this.k) {
      throw new IllegalArgumentException("Parameter k has to have the same value as " +
                                         "parameter k of the MDkNN-Tree!");
    }

    MDkNNTreeNode<O,D> root = (MDkNNTreeNode<O,D>) getRoot();
    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    doReverseKNNQuery(object.getID(), null, root, result);

    Collections.sort(result);
    return result;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createNewLeafNode() {
    return new MDkNNTreeNode<O, D>(file, leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  protected MTreeNode<O, D> createNewDirectoryNode() {
    return new MDkNNTreeNode<O, D>(file, dirCapacity, false);
  }

  /**
   * Creates a header for this M-Tree.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected MTreeHeader createHeader(int pageSize) {
    return new MDkNNTreeHeader(pageSize, dirCapacity, leafCapacity, k);
  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param q
   * @param node_entry
   * @param node
   * @param result
   */
  private void doReverseKNNQuery(Integer q,
                                 MDkNNDirectoryEntry<D> node_entry,
                                 MDkNNTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> entry = (MDkNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance()) <= 0)
          result.add(new QueryResult<D>(entry.getObjectID(), distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) node.getEntry(i);
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance() : distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(node_knnDist) <= 0) {
          MDkNNTreeNode<O, D> childNode = (MDkNNTreeNode<O, D>) getNode(entry.getNodeID());
          doReverseKNNQuery(q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Adapts the knn distances.
   *
   * @param node
   * @param q
   * @param knns_q
   */
  private D postInsert(MDkNNLeafEntry<D> q,
                       MDkNNDirectoryEntry<D> node_entry,
                       MDkNNTreeNode<O, D> node,
                       KNNList<D> knns_q) {


    D maxDist = distanceFunction.nullDistance();
    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> p = (MDkNNLeafEntry<D>) node.getEntry(i);
        D dist = distanceFunction.distance(p.getObjectID(), q.getObjectID());
        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist.compareTo(knns_q.getMaximumDistance()) <= 0) {
          QueryResult<D> qr = new QueryResult<D>(p.getObjectID(), dist);
          knns_q.add(qr);
          q.setKnnDistance(knns_q.getMaximumDistance());
        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist.compareTo(p.getKnnDistance()) <= 0) {
          List<QueryResult<D>> knns_p = doKNNQuery(p.getObjectID(), k);
          D knnDist_p = distanceFunction.nullDistance();

          for (QueryResult<D> r : knns_p) {
            D dist_pr = distanceFunction.distance(p.getObjectID(), r.getID());
            knnDist_p = Util.max(knnDist_p, dist_pr);
          }
          p.setKnnDistance(knnDist_p);
        }
        maxDist = Util.max(maxDist, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DistanceEntry> entries = getSortedEntries(node, q.getObjectID());
      for (DistanceEntry entry : entries) {
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance() : distanceFunction.infiniteDistance();
        if (entry.distance.compareTo(node_knnDist) < 0 ||
            entry.distance.compareTo(knns_q.getMaximumDistance()) < 0) {
          MDkNNTreeNode<O, D> childNode = (MDkNNTreeNode<O, D>) getNode(entry.entry.getNodeID());
          node_knnDist = postInsert(q, entry.entry, childNode, knns_q);
          if (node_entry != null)
            node_entry.setKnnDistance(node_knnDist);
        }
        maxDist = Util.max(maxDist, node_knnDist);
      }
    }
    return maxDist;
  }

  private List<DistanceEntry> getSortedEntries(MDkNNTreeNode<O, D> node, Integer q) {
    List<DistanceEntry> result = new ArrayList<DistanceEntry>();
    for (int i = 0; i < node.getNumEntries(); i++) {
      MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) node.getEntry(i);
      D distance = distanceFunction.distance(entry.getObjectID(), q);
      result.add(new DistanceEntry(entry, distance));
    }
    Collections.sort(result);
    return result;
  }

  private class DistanceEntry implements Comparable<DistanceEntry> {
    private MDkNNDirectoryEntry<D> entry;
    private D distance;

    public DistanceEntry(MDkNNDirectoryEntry<D> entry, D distance) {
      this.entry = entry;
      this.distance = distance;
    }

    /**
     * Compares this object with the specified object for order.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(DistanceEntry o) {
      int comp = this.distance.compareTo(o.distance);
      if (comp != 0) return comp;

      return this.entry.getObjectID().compareTo(o.entry.getObjectID());
    }


  }

}
