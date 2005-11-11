package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in a MCop-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkCoPTreeNode<O extends MetricalObject> extends MTreeNode<O, DoubleDistance> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkCoPTreeNode() {
  }

  /**
   * Creates a MCopTreeNode object.
   *
   * @param file     the file storing the MCop-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkCoPTreeNode(PageFile<MTreeNode<O, DoubleDistance>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkCoPTreeNode<O> createNewLeafNode(int capacity) {
    return new MkCoPTreeNode<O>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkCoPTreeNode<O> createNewDirectoryNode(int capacity) {
    return new MkCoPTreeNode<O>(file, capacity, false);
  }

  /**
   * Determines and returns the conservative approximation for the knn distances of this node
   * as the maximum of the conservative approximations of all entries.
   *
   * @return the conservative approximation for the knn distances
   */
  protected ApproximationLine conservativeKnnDistanceApproximation() {
    int k_max = ((MkCoPEntry) entries[0]).getK();

    // determine k_0, y_1, y_kmax
    int k_0 = 0;
    double y_1 = Double.NEGATIVE_INFINITY;
    double y_kmax = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < numEntries; i++) {
      MkCoPEntry entry = (MkCoPEntry) entries[i];
      ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
      k_0 = Math.max(approx.getK_0(), k_0);
    }

    for (int i = 0; i < numEntries; i++) {
      MkCoPEntry entry = (MkCoPEntry) entries[i];
      ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
      y_1 = Math.max(approx.getValueAt(k_0), y_1);
      y_kmax = Math.max(approx.getValueAt(k_max), y_kmax);
    }

    // determine m and t
    double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
    double t = y_1 - m * Math.log(k_0);

    // todo
    return new ApproximationLine(k_0, m, t);
  }

  /**
   * Determines and returns the progressive approximation for the knn distances of this node
   * as the maximum of the progressive approximations of all entries.
   *
   * @return the conservative approximation for the knn distances
   */
  protected ApproximationLine progressiveKnnDistanceApproximation() {
    int k_max = ((MkCoPEntry) entries[0]).getK();

    // determine y_1, y_kmax
    int k_0 = k_max;
    double y_1 = Double.POSITIVE_INFINITY;
    double y_kmax = Double.POSITIVE_INFINITY;

    for (int i = 0; i < numEntries; i++) {
      MkCoPEntry entry = (MkCoPEntry) entries[i];
      ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
      k_0 = Math.min(approx.getK_0(), k_0);
    }

    for (int i = 0; i < numEntries; i++) {
      MkCoPEntry entry = (MkCoPEntry) entries[i];
      ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
      y_1 = Math.min(approx.getValueAt(k_0), y_1);
      y_kmax = Math.min(approx.getValueAt(k_max), y_kmax);


//      System.out.println("\n   fffff y_1    " + y_1);
//      System.out.println("   fffff y_kmax " + y_kmax);
    }

//    System.out.println("aaaaaaaaa y_1 " + y_1);
//    System.out.println("aaaaaaaaa y_kmax " + y_kmax);

    // determine m and t
    double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
    double t = y_1 - m * Math.log(k_0);

    return new ApproximationLine(k_0, m, t);
  }
}
