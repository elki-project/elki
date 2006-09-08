package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.utilities.Enumeratable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a hierarchical axes parallel cluster that holds the ids of the objects
 * belonging to this cluster. A HierarchicalAxesParallelCluster can be visualized in a graph.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalAxesParallelCluster implements Comparable<HierarchicalAxesParallelCluster>, Enumeratable<HierarchicalAxesParallelCluster> {
  private final BitSet preferenceVector;
  private final List<Integer> ids;
  private final List<HierarchicalAxesParallelCluster> children;
  private final List<HierarchicalAxesParallelCluster> parents;
  private int level;
  private int levelIndex;
  private String label;

  /**
   * Provides a new empty hierarchical axes parallel cluster with the
   * specified preference vector.
   *
   * @param preferenceVector the preference vector of this cluster
   */
  public HierarchicalAxesParallelCluster(BitSet preferenceVector) {
    this(preferenceVector, new ArrayList<Integer>(), new ArrayList<HierarchicalAxesParallelCluster>(), new ArrayList<HierarchicalAxesParallelCluster>(), "", -1, -1);
  }

  /**
   * Provides a hierarchical axes parallel cluster with the
   * specified preference vector that holds the ids of the obejcts
   * belonging to this cluster.
   *
   * @param preferenceVector the preference vector of this cluster
   * @param ids              the idsa of the objects belonging to this cluster
   * @param children         the list of children of this cluster
   * @param parents          the list of parents of this cluster
   * @param label            the label of this cluster
   * @param level            the level of this cluster in the graph
   * @param levelIndex       the index of this cluster within the level
   */
  public HierarchicalAxesParallelCluster(BitSet preferenceVector,
                                         List<Integer> ids,
                                         List<HierarchicalAxesParallelCluster> children,
                                         List<HierarchicalAxesParallelCluster> parents,
                                         String label, int level, int levelIndex) {
    this.preferenceVector = preferenceVector;
    this.ids = ids;
    this.children = children;
    this.parents = parents;
    this.label = label;
    this.level = level;
    this.levelIndex = levelIndex;
  }

  /**
   * Adds a new id to this cluster.
   *
   * @param id the id to be added
   */
  public void addID(Integer id) {
    ids.add(id);
  }

  /**
   * Adds the specified list of ids to this cluster.
   *
   * @param ids the ids to be added
   */
  public void addIDs(List<Integer> ids) {
    this.ids.addAll(ids);
  }

  /**
   * Removes the specified id from the list of ids to this cluster.
   *
   * @param id the id to be removed
   */
  public void removeID(Integer id) {
    ids.remove(id);
  }

  /**
   * Adds a child cluster to this cluster.
   *
   * @param child the child to be added.
   */
  public void addChild(HierarchicalAxesParallelCluster child) {
    children.add(child);
  }

  /**
   * Returns the children of this cluster.
   *
   * @return the children of this cluster
   */
  public List<HierarchicalAxesParallelCluster> getChildren() {
    return children;
  }

  /**
   * Adds a parent to this cluster.
   *
   * @param parent the parent to be added.
   */
  public void addParent(HierarchicalAxesParallelCluster parent) {
    parents.add(parent);
  }

  /**
   * Returns the parents of this cluster.
   *
   * @return the parents of this cluster
   */
  public List<HierarchicalAxesParallelCluster> getParents() {
    return parents;
  }

  /**
   * Returns the preference vector of this cluster.
   *
   * @return the preference vector of this cluster
   */
  public BitSet getPreferenceVector() {
    return preferenceVector;
  }

  /**
   * Returns the level of this cluster.
   *
   * @return the level of this cluster
   */
  public int getLevel() {
    return level;
  }

  /**
   * Returns the index within the level of this cluster.
   *
   * @return the index within the level
   */
  public int getLevelIndex() {
    return levelIndex;
  }

  /**
   * Sets the index within the level of this cluster.
   *
   * @param levelIndex the index to be set
   */
  public void setLevelIndex(int levelIndex) {
    this.levelIndex = levelIndex;
  }

  /**
   * Sets the label of this cluster.
   *
   * @param label the label of this cluster
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Sets the level of this cluster.
   *
   * @param level the level to be set
   */
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

  /**
   * Returns the ids belonging to this cluster.
   *
   * @return the ids belonging to this cluster
   */
  public List<Integer> getIDs() {
    return ids;
  }

  /**
   * Compares this object with the specified object for order.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HierarchicalAxesParallelCluster o) {
    int comp = this.level - o.level;
    if (comp != 0) return -comp;

    return this.levelIndex - o.levelIndex;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final HierarchicalAxesParallelCluster that = (HierarchicalAxesParallelCluster) o;

    if (level != that.level) return false;
    return (levelIndex == that.levelIndex);
  }

  /**
   * Returns a hash code value for this cluster.
   *
   * @return a hash code value for this cluster
   */
  public int hashCode() {
    return preferenceVector.hashCode();
  }

  /**
   * Returns a copy of this cluster.
   *
   * @return a copy of this cluster
   */
  public HierarchicalAxesParallelCluster copy() {
    return new HierarchicalAxesParallelCluster(this.preferenceVector, this.ids, this.children, this.parents, this.label, this.level, this.levelIndex);
  }

  /**
   * Returns the number of children.
   *
   * @return the number of children
   */
  public int numChildren() {
    return children.size();
  }

  /**
   * Returns the child at the specified index.
   *
   * @param i the index of the child to be returned
   * @return the child at the specified index
   */
  public HierarchicalAxesParallelCluster getChild(int i) {
    return children.get(i);
  }
}
