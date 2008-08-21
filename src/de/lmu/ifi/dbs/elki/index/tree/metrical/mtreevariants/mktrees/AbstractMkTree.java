package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.KNNList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that provides the insertion of objects for all
 * M-Tree variants supporting processing of reverse k nearest neighbor queries by
 * using the k-nn distances of the entries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTree<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>>
    extends AbstractMTree<O, D, N, E> {

    /**
     * <p>Inserts the specified objects into this M-Tree sequentially
     * since a bulk load method is not implemented so far. <p/>
     * <p>Calls for each object
     * {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
     * AbstractMTree.insert(object, false)}. After insertion
     * a batch knn query is performed and the knn distances are adjusted.<p/>
     *
     * @see de.lmu.ifi.dbs.elki.index.Index#insert(java.util.List)
     */
    public final void insert(List<O> objects) {
        if (this.debug) {
            debugFine("insert " + objects + "\n");
        }

        if (!initialized) {
            initialize(objects.get(0));
        }

        List<Integer> ids = new ArrayList<Integer>();
        Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

        // insert sequentially
        for (O object : objects) {
            // create knnList for the object
            ids.add(object.getID());
            knnLists.put(object.getID(),
                new KNNList<D>(getK_max(), getDistanceFunction().infiniteDistance()));

            // insert the object
            super.insert(object, false);
        }

        // do batch nn
        batchNN(getRoot(), ids, knnLists);

        // adjust the knn distances
        distanceAdjustment(getRootEntry(), knnLists);

        if (debug) {
            getRoot().test(this, getRootEntry());
        }
    }

    /**
     * Returns the maximal number k of reverse
     * k nearest neighbors to be supported.
     *
     * @return the maximal number k of reverse
     *         k nearest neighbors to be supported
     */
    public abstract int getK_max();

    /**
     * Performs a distance adjustment in the subtree of the specified root entry.
     *
     * @param entry    the root entry of the current subtree
     * @param knnLists a map of knn lists for each leaf entry
     */
    public abstract void distanceAdjustment(E entry, Map<Integer, KNNList<D>> knnLists);
}
