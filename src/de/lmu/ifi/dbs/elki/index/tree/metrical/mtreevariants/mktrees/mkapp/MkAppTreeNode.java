package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.util.Arrays;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Represents a node in an MkApp-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MkAppEntry oneway - - contains
 * 
 * @param <O> object type
 * @param <D> distance type
 */
class MkAppTreeNode<O, D extends NumberDistance<D, ?>> extends AbstractMTreeNode<O, D, MkAppTreeNode<O, D>, MkAppEntry<D>> {
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
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public MkAppTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf, MkAppEntry.class);
  }

  /**
   * Determines and returns the polynomial approximation for the knn distances
   * of this node as the maximum of the polynomial approximations of all
   * entries.
   * 
   * @return the conservative approximation for the knn distances
   */
  protected PolynomialApproximation knnDistanceApproximation() {
    int p_max = 0;
    double[] b = null;
    for(int i = 0; i < getNumEntries(); i++) {
      MkAppEntry<D> entry = getEntry(i);
      PolynomialApproximation approximation = entry.getKnnDistanceApproximation();
      if(b == null) {
        p_max = approximation.getPolynomialOrder();
        b = new double[p_max];
      }
      for(int p = 0; p < p_max; p++) {
        b[p] += approximation.getB(p);
      }
    }

    for(int p = 0; p < p_max; p++) {
      b[p] /= p_max;
    }

    if(LoggingConfiguration.DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("b " + FormatUtil.format(b, 4));
      Logger.getLogger(this.getClass().getName()).fine(msg.toString());
    }

    return new PolynomialApproximation(b);
  }

  /**
   * Adjusts the parameters of the entry representing this node.
   * 
   * @param entry the entry representing this node
   * @param routingObjectID the id of the (new) routing object of this node
   * @param parentDistance the distance from the routing object of this node to
   *        the routing object of the parent node
   * @param mTree the M-Tree object holding this node
   */
  @Override
  public void adjustEntry(MkAppEntry<D> entry, DBID routingObjectID, D parentDistance, AbstractMTree<O, D, MkAppTreeNode<O, D>, MkAppEntry<D>> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // entry.setKnnDistanceApproximation(knnDistanceApproximation());
  }

  @Override
  protected void integrityCheckParameters(MkAppEntry<D> parentEntry, MkAppTreeNode<O, D> parent, int index, AbstractMTree<O, D, MkAppTreeNode<O, D>, MkAppEntry<D>> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);

    MkAppEntry<D> entry = parent.getEntry(index);
    PolynomialApproximation approximation_soll = knnDistanceApproximation();
    PolynomialApproximation approximation_ist = entry.getKnnDistanceApproximation();

    if(!Arrays.equals(approximation_ist.getCoefficients(), approximation_soll.getCoefficients())) {
      String soll = approximation_soll.toString();
      String ist = entry.getKnnDistanceApproximation().toString();
      throw new RuntimeException("Wrong polynomial approximation in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);

    }
  }
}