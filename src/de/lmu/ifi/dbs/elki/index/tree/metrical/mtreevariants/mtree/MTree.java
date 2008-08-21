package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

import java.util.List;

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
        // this.debug = true;
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
     * since no bulk load method is implemented so far.
     * Calls for each object
     * {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
     * AbstractMTree.insert(object, false)}.
     *
     * @see de.lmu.ifi.dbs.elki.index.Index#insert(java.util.List)
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
     *
     * @see de.lmu.ifi.dbs.elki.index.tree.TreeIndex#preInsert(de.lmu.ifi.dbs.elki.index.tree.Entry)
     */
    protected void preInsert(MTreeEntry<D> entry) {
        // do nothing
    }

    /**
     * @return a new MTreeLeafEntry
     * @see AbstractMTree#createNewLeafEntry(DatabaseObject,Distance)
     */
    protected MTreeEntry<D> createNewLeafEntry(O object, D parentDistance) {
        return new MTreeLeafEntry<D>(object.getID(), parentDistance);
    }

    /**
     * @return a new MTreeDirectoryEntry
     * @see AbstractMTree#createNewDirectoryEntry(AbstractMTreeNode,Integer,Distance)
     */
    protected MTreeEntry<D> createNewDirectoryEntry(MTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
        return new MTreeDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(),
            node.coveringRadius(routingObjectID, this));
    }

    /**
     * Creates an entry representing the root node.
     *
     * @return a new MTreeDirectoryEntry by calling
     *         <code>new MTreeDirectoryEntry<D>(null, null, 0, null)</code>
     * @see de.lmu.ifi.dbs.elki.index.tree.TreeIndex#createRootEntry()
     */
    protected MTreeEntry<D> createRootEntry() {
        return new MTreeDirectoryEntry<D>(null, null, 0, null);
    }

    /**
     * @return a new MTreeNode which is a leaf node
     * @see de.lmu.ifi.dbs.elki.index.tree.TreeIndex#createNewLeafNode(int)
     */
    protected MTreeNode<O, D> createNewLeafNode(int capacity) {
        return new MTreeNode<O, D>(file, capacity, true);
    }

    /**
     * @return a new MTreeNode which is a directory node
     * @see de.lmu.ifi.dbs.elki.index.tree.TreeIndex#createNewDirectoryNode(int)
     */
    protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
        return new MTreeNode<O, D>(file, capacity, false);
    }
}
