package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the given parameter.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MkTreeHeader oneway
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTreeUnified<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMkTree<O, D, N, E> {
  /**
   * Holds the maximum value of k to support.
   */
  protected int k_max;

  /**
   * Constructor.
   * 
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   * @param k_max Maximum value for k
   */
  public AbstractMkTreeUnified(String fileName, int pageSize, long cacheSize, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceQuery, distanceFunction);
    this.k_max = k_max;
  }

  /**
   * <p>
   * Inserts the specified objects into this M-Tree sequentially since a bulk
   * load method is not implemented so far.
   * <p/>
   * <p>
   * Calls for each object
   * {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
   * AbstractMTree.insert(object, false)}. After insertion a batch knn query is
   * performed and the knn distances are adjusted.
   * <p/>
   */
  @Override
  public final void insert(List<O> objects) {
    if(objects.isEmpty()) {
      return;
    }

    if(getLogger().isDebugging()) {
      getLogger().debugFine("insert " + objects + "\n");
    }

    if(!initialized) {
      initialize(objects.get(0));
    }

    ModifiableDBIDs ids = DBIDUtil.newArray();
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();

    // insert sequentially
    for(O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNHeap<D>(k_max, getDistanceFactory().infiniteDistance()));

      // insert the object
      super.insert(object, false);
    }

    // do batch nn
    batchNN(getRoot(), ids, knnLists);

    // adjust the knn distances
    kNNdistanceAdjustment(getRootEntry(), knnLists);

    if(extraIntegrityChecks) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * @return a new {@link MkTreeHeader}
   */
  @Override
  protected TreeIndexHeader createHeader() {
    return new MkTreeHeader(pageSize, dirCapacity, leafCapacity, k_max);
  }

  /**
   * Performs a distance adjustment in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  protected abstract void kNNdistanceAdjustment(E entry, Map<DBID, KNNHeap<D>> knnLists);
}