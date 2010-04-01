package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Represents a node in an M-Tree.
 *
 * @author Elke Achtert
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeNode<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeNode<O, D, MTreeNode<O, D>, MTreeEntry<D>> {
    private static final long serialVersionUID = 1;

    /**
     * Empty constructor for Externalizable interface.
     */
    public MTreeNode() {
        // empty constructor
    }

    /**
     * Creates a new MTreeNode with the specified parameters.
     *
     * @param file     the file storing the M-Tree
     * @param capacity the capacity (maximum number of entries plus 1 for overflow)
     *                 of this node
     * @param isLeaf   indicates whether this node is a leaf node
     */
    public MTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
        super(file, capacity, isLeaf, MTreeEntry.class);
    }

    /**
     * @return a new MTreeNode which is a leaf node
     */
    @Override
    protected MTreeNode<O, D> createNewLeafNode(int capacity) {
        return new MTreeNode<O, D>(getFile(), capacity, true);
    }

    /**
     * @return a new MTreeNode which is a directory node
     */
    @Override
    protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
        return new MTreeNode<O, D>(getFile(), capacity, false);
    }
}
