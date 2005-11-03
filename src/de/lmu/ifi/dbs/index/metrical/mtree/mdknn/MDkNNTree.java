package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.MTree;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;

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
    super(fileName, cacheSize);
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
   */
  public MDkNNTree(String fileName, int pageSize, int cacheSize, DistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, distanceFunction);
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    MDkNNLeafEntry<D> newEntry = (MDkNNLeafEntry<D>) doInsert(object);
//    preInsert();
  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object           the query object
   * @param k                the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<D>> kNNQuery(O object, int k) {
    if (k != this.k) {
      throw new IllegalArgumentException("Parameter k must have the same value" +
                                         "as parameter k of the MDkNN-Tree!");
    }

    return doKNNQuery(object.getID()).toList();
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
   * Adapts the knn distances.
   *
   * @param node
   * @param q
   * @param knnList
   */
  private void preInsert(MDkNNTreeNode<O, D> node, MDkNNLeafEntry<D> q, KNNList<D> knnList,
                         DistanceFunction<O, D> distanceFunction) {
    D d_k = knnList.getMaximumDistance();
    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> p = (MDkNNLeafEntry<D>) node.getEntry(i);
        D dist = distanceFunction.distance(p.getObjectID(), q.getObjectID());
        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist.compareTo(d_k) <= 0) {
          QueryResult<D> qr = new QueryResult<D>(p.getObjectID(), dist);
          knnList.add(qr);
          q.setKnnDistance(knnList.getMaximumDistance());
        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist.compareTo(p.getKnnDistance()) <= 0) {
          KNNList knn_p = doKNNQuery(p.getObjectID());

        }
      }
    }
    // directory node
    else {
    }

  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param q           the id of the query object
   * @return a List of the query results
   */
  private KNNList<D> doKNNQuery(Integer q) {
    // variables
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();
    final KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID.value(), null));
    D d_k = knnList.getMaximumDistance();

    /*
    // search in tree
    while (!pq.isEmpty()) {
      PQNode pqNode = (PQNode) pq.getMinNode();

      if (pqNode.getKey().compareTo(d_k) > 0) {
        return knnList;
      }

      MTreeNode<O, D> node = getNode(pqNode.getValue().getID());
      Integer o_p = pqNode.routingObjectID;

      // directory node
      if (! node.isLeaf) {
        for (int i = 0; i < node.numEntries; i++) {
          DirectoryEntry<D> entry = (DirectoryEntry<D>) node.entries[i];
          Integer o_r = entry.getObjectID();
          D r_or = entry.getCoveringRadius();
          D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
          D d2 = o_p != null ? distanceFunction.distance(o_r, o_p) : distanceFunction.nullDistance();

          D diff = d1.compareTo(d2) > 0 ?
                   d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if (diff.compareTo(sum) <= 0) {
            D d3 = distanceFunction.distance(o_r, q);
            D d_min = Util.max(d3.minus(r_or), distanceFunction.nullDistance());
            if (d_min.compareTo(d_k) <= 0) {
              pq.addNode(new PQNode(d_min, entry.getNodeID(), o_r));
            }
          }
        }

      }

      // data node
      else {
        for (int i = 0; i < node.numEntries; i++) {
          Entry<D> entry = node.entries[i];
          Integer o_j = entry.getObjectID();

          D d1 = distanceFunction.distance(o_p, q);
          D d2 = distanceFunction.distance(o_j, o_p);

          D diff = d1.compareTo(d2) > 0 ?
                   d1.minus(d2) : d2.minus(d1);

          if (diff.compareTo(d_k) <= 0) {
            D d3 = distanceFunction.distance(o_j, q);
            if (d3.compareTo(d_k) <= 0) {
              QueryResult<D> queryResult = new QueryResult<D>(o_j, d3);
              knnList.add(queryResult);
              if (knnList.size() == k)
                d_k = knnList.getMaximumDistance();
            }
          }
        }
      }
    }
    */

    return knnList;
  }

}
