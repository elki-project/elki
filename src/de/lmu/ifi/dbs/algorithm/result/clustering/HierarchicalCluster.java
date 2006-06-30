package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * // todo comment all
 * Provides a hierarchical cluster that holds the ids of the obejcts belonging to this cluster,
 *
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalCluster implements Comparable<HierarchicalCluster> {
  private final BitSet preferenceVector;
  private final List<Integer> ids;
  private final List<HierarchicalCluster> children;
  private int level;
  private int levelIndex;
  private String label;

  public HierarchicalCluster(BitSet preferenceVector) {
    this(preferenceVector, new ArrayList<Integer>(), new ArrayList<HierarchicalCluster>(), "", -1, -1);
  }

  public HierarchicalCluster(BitSet preferenceVector, List<Integer> ids, List<HierarchicalCluster> children, String label, int level, int levelIndex) {
    this.preferenceVector = preferenceVector;
    this.ids = ids;
    this.children = children;
    this.label = label;
    this.level = level;
    this.levelIndex = levelIndex;
  }

  public void addID(Integer id) {
    ids.add(id);
  }

  public void addChild(HierarchicalCluster child) {
    children.add(child);
  }

  public List<HierarchicalCluster> getChildren() {
    return children;
  }

  public BitSet getPreferenceVector() {
    return preferenceVector;
  }

  public int getLevel() {
    return level;
  }

  public int getLevelIndex() {
    return levelIndex;
  }

  public void setLevelIndex(int levelIndex) {
    this.levelIndex = levelIndex;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return label;
  }

  public List<Integer> getIds() {
    return ids;
  }

  /**
   * Compares this object with the specified object for order.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HierarchicalCluster o) {
    int comp = this.level - o.level;
    if (comp != 0) return -comp;

    return this.levelIndex - o.levelIndex;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final HierarchicalCluster that = (HierarchicalCluster) o;

    if (level != that.level) return false;
    return (levelIndex == that.levelIndex);
//    if (!children.equals(that.children)) return false;
//    if (!ids.equals(that.ids)) return false;
//    if (!label.equals(that.label)) return false;
//    return (!preferenceVector.equals(that.preferenceVector));
  }

  public int hashCode() {
    return preferenceVector.hashCode();
  }

  public HierarchicalCluster copy() {
    return new HierarchicalCluster(this.preferenceVector, this.ids, this.children, this.label, this.level, this.levelIndex);
  }
}
