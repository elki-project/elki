package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.util.Arrays;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Represents a node in an MkApp-Tree.
 *
 * @author Elke Achtert
 */
class MkAppTreeNode<O extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number>
    extends AbstractMTreeNode<O, D, MkAppTreeNode<O, D, N>, MkAppEntry<D, N>> {
    private static final long serialVersionUID = 1;

    /**
     * Empty constructor for Externalizable interface.
     */
    public MkAppTreeNode() {
        // empty constructor
    }

    /**
     * Creates a MkAppTreeNode object.
     *
     * @param file     the file storing the MCop-Tree
     * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
     * @param isLeaf   indicates whether this node is a leaf node
     */
    public MkAppTreeNode(PageFile<MkAppTreeNode<O, D, N>> file, int capacity, boolean isLeaf) {
        super(file, capacity, isLeaf, MkAppEntry.class);
    }

    /**
     * Creates a new leaf node with the specified capacity.
     *
     * @param capacity the capacity of the new node
     * @return a new leaf node
     */
    @Override
    protected MkAppTreeNode<O, D, N> createNewLeafNode(int capacity) {
        return new MkAppTreeNode<O, D, N>(getFile(), capacity, true);
    }

    /**
     * Creates a new directory node with the specified capacity.
     *
     * @param capacity the capacity of the new node
     * @return a new directory node
     */
    @Override
    protected MkAppTreeNode<O, D, N> createNewDirectoryNode(int capacity) {
        return new MkAppTreeNode<O, D, N>(getFile(), capacity, false);
    }

    /**
     * Determines and returns the polynomial approximation for the knn distances of this node
     * as the maximum of the polynomial approximations of all entries.
     *
     * @return the conservative approximation for the knn distances
     */
    protected PolynomialApproximation knnDistanceApproximation() {
        int p_max = 0;
        double[] b = null;
        for (int i = 0; i < getNumEntries(); i++) {
            MkAppEntry<D, N> entry = getEntry(i);
            PolynomialApproximation approximation = entry.getKnnDistanceApproximation();
            if (b == null) {
                p_max = approximation.getPolynomialOrder();
                b = new double[p_max];
            }
            for (int p = 0; p < p_max; p++) {
                b[p] += approximation.getB(p);
            }
        }

        for (int p = 0; p < p_max; p++) {
            b[p] /= p_max;
        }

        if (LoggingConfiguration.DEBUG) {
            StringBuffer msg = new StringBuffer();
            msg.append("b " + FormatUtil.format(b, 4));
            Logger.getLogger(this.getClass().getName()).fine(msg.toString());
        }

        return new PolynomialApproximation(b);
    }

    /**
     * Adjusts the parameters of the entry representing this node.
     *
     * @param entry           the entry representing this node
     * @param routingObjectID the id of the (new) routing object of this node
     * @param parentDistance  the distance from the routing object of this node
     *                        to the routing object of the parent node
     * @param mTree           the M-Tree object holding this node
     */
    @Override
    public void adjustEntry(MkAppEntry<D, N> entry, Integer routingObjectID, D parentDistance,
                            AbstractMTree<O, D, MkAppTreeNode<O, D, N>, MkAppEntry<D, N>> mTree) {
        super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
//    entry.setKnnDistanceApproximation(knnDistanceApproximation());
    }

    @Override
    protected void integrityCheckParameters(MkAppEntry<D, N> parentEntry, MkAppTreeNode<O, D, N> parent, int index,
                        AbstractMTree<O, D, MkAppTreeNode<O, D, N>, MkAppEntry<D, N>> mTree) {
        super.integrityCheckParameters(parentEntry, parent, index, mTree);

        MkAppEntry<D, N> entry = parent.getEntry(index);
        PolynomialApproximation approximation_soll = knnDistanceApproximation();
        PolynomialApproximation approximation_ist = entry.getKnnDistanceApproximation();

        if (!Arrays.equals(approximation_ist.getCoefficients(), approximation_soll.getCoefficients())) {
            String soll = approximation_soll.toString();
            String ist = entry.getKnnDistanceApproximation().toString();
            throw new RuntimeException("Wrong polynomial approximation in node "
                + parent.getID() + " at index " + index + " (child "
                + entry.getID() + ")" + "\nsoll: " + soll
                + ",\n ist: " + ist);

        }

    }
}
