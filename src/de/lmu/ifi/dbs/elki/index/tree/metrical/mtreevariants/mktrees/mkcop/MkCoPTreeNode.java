package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Represents a node in an MkCop-Tree.
 *
 * @author Elke Achtert
 */
class MkCoPTreeNode<O extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number>
    extends AbstractMTreeNode<O, D, MkCoPTreeNode<O, D, N>, MkCoPEntry<D, N>> {
    private static final long serialVersionUID = 1;

    /**
     * Empty constructor for Externalizable interface.
     */
    public MkCoPTreeNode() {
        // empty constructor
    }

    /**
     * Creates a MkCoPTreeNode object.
     *
     * @param file     the file storing the MCop-Tree
     * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
     * @param isLeaf   indicates wether this node is a leaf node
     */
    public MkCoPTreeNode(PageFile<MkCoPTreeNode<O, D, N>> file, int capacity, boolean isLeaf) {
        super(file, capacity, isLeaf);
    }

    /**
     * Creates a new leaf node with the specified capacity.
     *
     * @param capacity the capacity of the new node
     * @return a new leaf node
     */
    @Override
    protected MkCoPTreeNode<O, D, N> createNewLeafNode(int capacity) {
        return new MkCoPTreeNode<O, D, N>(getFile(), capacity, true);
    }

    /**
     * Creates a new directory node with the specified capacity.
     *
     * @param capacity the capacity of the new node
     * @return a new directory node
     */
    @Override
    protected MkCoPTreeNode<O, D, N> createNewDirectoryNode(int capacity) {
        return new MkCoPTreeNode<O, D, N>(getFile(), capacity, false);
    }

    /**
     * Determines and returns the conservative approximation for the knn distances of this node
     * as the maximum of the conservative approximations of all entries.
     *
     * @param k_max the maximum k parameter
     * @return the conservative approximation for the knn distances
     */
    protected ApproximationLine conservativeKnnDistanceApproximation(int k_max) {
        // determine k_0, y_1, y_kmax
        int k_0 = k_max;
        double y_1 = Double.NEGATIVE_INFINITY;
        double y_kmax = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < getNumEntries(); i++) {
            MkCoPEntry<D, N> entry = getEntry(i);
            ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
            k_0 = Math.min(approx.getK_0(), k_0);
        }

        for (int i = 0; i < getNumEntries(); i++) {
            MkCoPEntry<D, N> entry = getEntry(i);
            ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
            double entry_y_1 = approx.getValueAt(k_0);
            double entry_y_kmax = approx.getValueAt(k_max);
            if (!Double.isInfinite(entry_y_1)) {
                y_1 = Math.max(entry_y_1, y_1);
            }

            if (!Double.isInfinite(entry_y_kmax)) {
                y_kmax = Math.max(entry_y_kmax, y_kmax);
            }
        }

        if (debug) {
            StringBuffer msg = new StringBuffer();
            msg.append("k_0 " + k_0);
            msg.append("k_max " + k_max);
            msg.append("y_1 " + y_1);
            msg.append("y_kmax " + y_kmax);
            debugFine(msg.toString());
        }

        // determine m and t
        double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
        double t = y_1 - m * Math.log(k_0);

        return new ApproximationLine(k_0, m, t);
    }

    /**
     * Determines and returns the progressive approximation for the knn distances of this node
     * as the maximum of the progressive approximations of all entries.
     *
     * @param k_max the maximum k parameter
     * @return the conservative approximation for the knn distances
     */
    protected ApproximationLine progressiveKnnDistanceApproximation(int k_max) {
        if (!isLeaf()) {
            throw new UnsupportedOperationException("Progressive KNN-distance approximation " +
                "is only vailable in leaf nodes!");
        }

        // determine k_0, y_1, y_kmax
        int k_0 = 0;
        double y_1 = Double.POSITIVE_INFINITY;
        double y_kmax = Double.POSITIVE_INFINITY;

        for (int i = 0; i < getNumEntries(); i++) {
            MkCoPLeafEntry<D, N> entry = (MkCoPLeafEntry<D, N>) getEntry(i);
            ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
            k_0 = Math.max(approx.getK_0(), k_0);
        }

        for (int i = 0; i < getNumEntries(); i++) {
            MkCoPLeafEntry<D, N> entry = (MkCoPLeafEntry<D, N>) getEntry(i);
            ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
            y_1 = Math.min(approx.getValueAt(k_0), y_1);
            y_kmax = Math.min(approx.getValueAt(k_max), y_kmax);
        }

        // determine m and t
        double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
        double t = y_1 - m * Math.log(k_0);

        return new ApproximationLine(k_0, m, t);
    }

    @Override
    public void adjustEntry(MkCoPEntry<D, N> entry, Integer routingObjectID, D parentDistance,
                            AbstractMTree<O, D, MkCoPTreeNode<O, D, N>, MkCoPEntry<D, N>> mTree) {
        super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
        // adjust conservative distance approximation
//    int k_max = ((MkCoPTree<O,D>) mTree).getK_max();
//    entry.setConservativeKnnDistanceApproximation(conservativeKnnDistanceApproximation(k_max));
    }

    @Override
    protected void test(MkCoPEntry<D, N> parentEntry, MkCoPTreeNode<O, D, N> parent, int index,
                        AbstractMTree<O, D, MkCoPTreeNode<O, D, N>, MkCoPEntry<D, N>> mTree) {
        super.test(parentEntry, parent, index, mTree);
        // test conservative approximation
        MkCoPEntry<D, N> entry = parent.getEntry(index);
        int k_max = ((MkCoPTree<O, D, N>) mTree).getK_max();
        ApproximationLine approx = conservativeKnnDistanceApproximation(k_max);
        if (!entry.getConservativeKnnDistanceApproximation().equals(approx)) {
            String soll = approx.toString();
            String ist = entry.getConservativeKnnDistanceApproximation().toString();
            throw new RuntimeException("Wrong conservative approximation in node "
                + parent.getID() + " at index " + index + " (child "
                + entry.getID() + ")" + "\nsoll: " + soll
                + ",\n ist: " + ist);
        }
    }
}
