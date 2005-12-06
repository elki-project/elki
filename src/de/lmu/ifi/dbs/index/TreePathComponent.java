package de.lmu.ifi.dbs.index;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class TreePathComponent<N extends Node> {
  /**
   * The node.
   */
  private N node;

  /**
   * The index of the node in its parent if the node is not a root node, null otherwise.
   */
  private Integer index;

  /**
   * Creates a new TreePathComponent.
   *
   * @param node  the node
   * @param index the index of the node in its parent if the node is not a root node, null otherwise
   */
  public TreePathComponent(N node, Integer index) {
    this.node = node;
    this.index = index;
  }

  /**
   * Returns the node of this TreePathComponent.
   *
   * @return the node
   */
  public N getNode() {
    return node;
  }

  /**
   * Returns the index of the node in its parent if the node is not a root node, null otherwise
   *
   * @return the index of the node in its parent
   */
  public Integer getIndex() {
    return index;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if the node of this component has the same
   * id as the node of the o argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TreePathComponent that = (TreePathComponent) o;
    return (node.getID().equals(that.node.getID()));
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  public int hashCode() {
    return node.getID();
  }


}
