package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the specified parameter {@link #K_MAX_PARAM}.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTreeUnified<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMkTree<O, D, N, E> {
  /**
   * OptionID for {@link #K_MAX_PARAM}.
   */
  public static final OptionID K_MAX_ID = OptionID.getOrCreateOptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

  /**
   * Parameter specifying the maximal number k of reverse k nearest neighbors to
   * be supported, must be an integer greater than 0.
   * <p>
   * Key: {@code -mktree.kmax}
   * </p>
   */
  public final IntParameter K_MAX_PARAM = new IntParameter(K_MAX_ID, new GreaterConstraint(0));

  /**
   * Holds the value of parameter {@link #K_MAX_PARAM}.
   */
  protected int k_max;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractMkTreeUnified(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(K_MAX_PARAM)) {
      k_max = K_MAX_PARAM.getValue();
    }
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