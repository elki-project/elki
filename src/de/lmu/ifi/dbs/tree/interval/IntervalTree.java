package de.lmu.ifi.dbs.tree.interval;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.*;

import java.util.BitSet;
import java.util.Set;

/**
 * Provides a tree consisting of subtrees of intervals.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class IntervalTree extends AbstractLoggable implements Enumeratable<IntervalTree>, Identifiable<IntervalTree> {
  /**
   * Used for id assignment.
   */
  private static int ID = 0;

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
  private Set<Integer> ids;

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
   * The unique id of this interval.
   */
  private int intervalID;

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
  public IntervalTree(HyperBoundingBox interval, BitSet representation, Set<Integer> ids, int level) {
    super(LoggingConfiguration.DEBUG);
//    this.debug = true;
    this.interval = interval;
    this.representation = representation;
    this.ids = ids;
    this.level = level;
    this.children = null;
    this.intervalID = ++ID;
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

      Set<Integer> childIDs = split.split(getIds(), childInterval, childLevel);
      if (childIDs != null) {
        children[numChildren++] = new IntervalTree(childInterval, childRepresentation, childIDs, childLevel);
        if (debug) {
          msg.append("\n\n" + children[numChildren - 1].toString());
          msg.append("\nids: " + childIDs.size());
        }
      }
    }
    if (debug) {
      msg.append("\nlevel: " + childLevel);
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
  public Set<Integer> getIds() {
    return ids;
  }

  /**
   * Removes the specified ids from this interval.
   *
   * @param ids the set of ids to be removed
   */
  public void removeIDs(Set ids) {
    this.ids.removeAll(ids);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return "ids: " + ids.size() + " (" + level + ")";
//    return formatRepresentation() + ", ids: " + ids.size() + ", level " + level;
//    return formatRepresentation() + " = " + getID() +
//           "\nlevel: " + level +
//           "\ninterval: " + interval +
//           "\nids: " + ids.size();// +
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

  /**
   * Returns interval (i.e. the min and max values in each dimension) represented by this subtree.
   *
   * @return the interval represented by this subtree
   */
  public HyperBoundingBox getInterval() {
    return interval;
  }

  /**
   * Returns the level of this subtree.
   *
   * @return the level of this subtree
   */
  public int getLevel() {
    return level;
  }

  /**
   * Returns the unique id of this object.
   *
   * @return the unique id of this object
   */
  public Integer getID() {
    return intervalID;
  }

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(Identifiable<IntervalTree> o) {
    IntervalTree other = (IntervalTree) o;

    int myPriority = getPriority();
    int otherPriority = other.getPriority();
    if (myPriority < otherPriority) return -1;
    if (myPriority > otherPriority) return +1;

    if (this.level > other.level) return -1;
    if (this.level < other.level) return +1;

    if (this.ids.size() < other.ids.size()) return -1;
    if (this.ids.size() > other.ids.size()) return +1;

    if (this.getID() < other.getID()) return -1;
    if (this.getID() > other.getID()) return +1;

    return 0;
  }

  /**
   * Returns the priority of this interval.
   * @return the priority of this interval
   */
  public int getPriority() {
//    return this.ids.size();
    return (this.level + 1) * this.ids.size();
  }
}
