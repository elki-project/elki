package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 */
public class MTree<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractMTree<O, D, MTreeNode<O, D>, MTreeEntry<D>> {

    /**
     * Provides a new M-Tree.
     */
    public MTree() {
        super();
        this.debug = true;
    }

    /**
     * Inserts the specified object into this M-Tree
     * by calling {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
     * AbstractMTree.insert(object, false)}.
     *
     * @param object the object to be inserted
     */
    public void insert(O object) {
        this.insert(object, false);
    }

    /**
     * Inserts the specified objects into this M-Tree sequentially
     * since a bulk load method is not implemented so far.
     * Calls for each object
     * {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
     * AbstractMTree.insert(object, false)}.
     */
    // todo: bulk load method
    public void insert(List<O> objects) {
        for (O object : objects) {
            insert(object, false);
        }

        if (debug) {
            getRoot().test(this, getRootEntry());
        }
    }

    /**
     * Does nothing because no operations are necessary before inserting an entry.
     */
    @Override
    protected void preInsert(MTreeEntry<D> entry) {
        // do nothing
    }

    /**
     * Throws an UnsupportedOperationException since
     * reverse knn queries are not yet supported by an M-Tree.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
        throw new UnsupportedOperationException("Reverse knn-queries are not yet supported!");
    }

    @Override
    protected void initializeCapacities(O object, boolean verbose) {
        D dummyDistance = getDistanceFunction().nullDistance();
        int distanceSize = dummyDistance.externalizableSize();

        // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
        double overhead = 12.125;
        if (pageSize - overhead < 0) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
        // coveringRadius + parentDistance) + 1
        dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize) + 1;

        if (dirCapacity <= 1) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        if (dirCapacity < 10) {
            warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
        }
        // leafCapacity = (pageSize - overhead) / (objectID + parentDistance) +
        // 1
        leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize) + 1;

        if (leafCapacity <= 1) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        if (leafCapacity < 10) {
            warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
        }

        if (verbose) {
            verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
        }
    }

    /**
     * @return a new MTreeLeafEntry representing the specified data object
     */
    @Override
    protected MTreeEntry<D> createNewLeafEntry(O object, D parentDistance) {
        return new MTreeLeafEntry<D>(object.getID(), parentDistance);
    }

    /**
     * @return a new MTreeDirectoryEntry representing the specified node
     */
    @Override
    protected MTreeEntry<D> createNewDirectoryEntry(MTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
        return new MTreeDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(),
            node.coveringRadius(routingObjectID, this));
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
}
