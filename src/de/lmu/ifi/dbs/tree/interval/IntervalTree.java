package de.lmu.ifi.dbs.tree.interval;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.tree.Enumeratable;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a tree consisting of subtrees of intervals.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class IntervalTree extends AbstractLoggable implements Enumeratable<IntervalTree> {
  /**
   * The level of this subtree, 0 indicates the root level.
   */
  private int level;

  /**
   * The interval (i.e. the min and max values in each dimension) represented by this subtree.
   */
  private HyperBoundingBox interval;

  /**
   * The ids of the objects associated with the interval represented by this subtree;
   */
  private List<Integer> ids;

  /**
   * The bitwise representation of this subtree. The representation has a length of
   * (level + 1) * dimensionality. For each level a 0 indicates that the respective dimension
   * has been splitted right, a 1 indicates that the respective dimension has been splitted left.
   */
  private BitSet representation;

  /**
   * Holds the child subtrees.
   */
  private IntervalTree[] children;

  /**
   * Holds the number of children.
   */
  private int numChildren;

  /**
   * Empty constructor for externalizable interface.
   */
  public IntervalTree() {
    this(null, null, null, 0);
  }

  /**
   * Provides a tree consisting of subtrees of intervals.
   *
   * @param interval       the interval represented by this subtree
   * @param representation the bitwise representation of this subtree.
   * @param ids            the ids of the objects associated with the interval represented by this subtree
   * @param level          the level of this subtree
   */
  public IntervalTree(HyperBoundingBox interval, BitSet representation, List<Integer> ids, int level) {
    super(LoggingConfiguration.DEBUG);
//    super(true);
    this.interval = interval;
    this.representation = representation;
    this.ids = ids;
    this.level = level;
    this.children = null;
  }

  /**
   * Splits this subtree into 2^dimensionality children.
   *
   * @param split the split object
   */
  public void performSplit(IntervalTreeSplit split) {
    if (children != null) return;

    StringBuffer msg = new StringBuffer();
    if (debug) {
      msg.append(this);
    }

    int dim = interval.getDimensionality();
    int childLevel = level + 1;

    int maxNumChildren = (int) Math.pow(2, dim);
    children = new IntervalTree[maxNumChildren];
    for (int d = 0; d < maxNumChildren; d++) {
      BitSet childRepresentation = (BitSet) this.representation.clone();
      BitSet dRep = Util.int2Bit(d);
      // create child interval
      double[] min = new double[dim];
      double[] max = new double[dim];
      for (int i = 0; i < dim; i++) {
        double splitPoint = interval.getMin(i + 1) + (interval.getMax(i + 1) - interval.getMin(i + 1)) / 2;
        if (dRep.get(i)) {
          childRepresentation.set((level + 1) * dim + i);
          min[i] = splitPoint;
          max[i] = interval.getMax(i + 1);
        }
        else {
          min[i] = interval.getMin(i + 1);
          max[i] = splitPoint;
        }
      }
      HyperBoundingBox childInterval = new HyperBoundingBox(min, max);

      List<Integer> childIDs = split.split(getIds(), childInterval, childLevel);
      if (childIDs != null) {
        children[numChildren++] = new IntervalTree(childInterval, childRepresentation, childIDs, childLevel);
        if (debug) {
          msg.append("\n\n" + children[numChildren - 1].toString());
          msg.append("\nids: " + childIDs.size());
        }
      }
    }
    if (debug) {
      debugFine(msg.toString());
    }
  }

  /**
   * Returns the number of children.
   *
   * @return the number of children
   */
  public int numChildren() {
    return numChildren;
  }

  /**
   * Returns the child at the specified index.
   *
   * @param index the index of the child to be returned
   * @return the child at the specified index
   */
  public IntervalTree getChild(int index) {
    return children[index];
  }

  /**
   * Returns the ids associated with this subtree.
   *
   * @return the ids associated with this subtree
   */
  public List<Integer> getIds() {
    return ids;
  }

  public static void main(String[] args) {
    int dim = 2;
    double[] min = new double[dim];
    double[] max = new double[dim];
    Arrays.fill(max, 1);
    HyperBoundingBox interval = new HyperBoundingBox(min, max);

    BitSet representation = new BitSet();
    IntervalTree n = new IntervalTree(interval, representation, new ArrayList<Integer>(), 0);

    IntervalTreeSplit split = new IntervalTreeSplit() {
      public List<Integer> split(List<Integer> parentIDs, HyperBoundingBox childInterval, int childLevel) {
        return parentIDs;
      }
    };

    n.performSplit(split);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return formatRepresentation() +
           "\ninterval: " + interval +
           "\nids: " + ids.size();// +
//           "\nchildren: " + (children == null ? children : Arrays.asList(children));
  }

  /**
   * Returns a string representation of the specified bit set.
   *
   * @return a string representation of the specified bit set.
   */
  public String formatRepresentation() {
    StringBuffer msg = new StringBuffer();
    int dim = (level + 1) * interval.getDimensionality();
    for (int d = 0; d < dim; d++) {
      if (d != 0 && d % interval.getDimensionality() == 0) {
        msg.append("-");
      }
      if (representation.get(d)) {
        msg.append("1");
      }
      else {
        msg.append("0");
      }

    }

    return msg.toString();
  }

  public BreadthFirstEnumeration<IntervalTree> breadthFirstEnumeration() {
    return new BreadthFirstEnumeration<IntervalTree>(this);
  }

  public boolean hasMoreChildren() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
