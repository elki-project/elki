package de.lmu.ifi.dbs.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a path to a node in a tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class TreePath {
  /**
   * Path representing the parent, null if lastPathComponent represents
   * the root.
   */
  private TreePath parentPath;
  
  /**
   * Last path component.
   */
  private TreePathComponent lastPathComponent;

  /**
   * Constructs a path from a list of path components, uniquely identifying
   * the path from the root of the tree to a specific node.
   * The first element in the path is the root of the
   * tree, the last node is the node identified by the path.
   *
   * @param path a list of TreePathComponents representing the path to a node
   */
  public TreePath(List<TreePathComponent> path) {
    if (path == null || path.size() == 0)
      throw new IllegalArgumentException("path in TreePath must be non null and not empty.");
    lastPathComponent = path.get(path.size() - 1);
    if (path.size() > 1)
      parentPath = new TreePath(path, path.size() - 1);
  }

  /**
   * Constructs a TreePath containing only a single element. This is
   * usually used to construct a TreePath for the the root of the tree.
   *
   * @param singlePath a TreePathComponent representing the path to a node
   */
  public TreePath(TreePathComponent singlePath) {
    if (singlePath == null)
      throw new IllegalArgumentException("path in TreePath must be non null.");
    lastPathComponent = singlePath;
    parentPath = null;
  }

  /**
   * Constructs a new TreePath, which is the path identified by
   * <code>parent</code> ending in <code>lastElement</code>.
   */
  protected TreePath(TreePath parent, TreePathComponent lastElement) {
    if (lastElement == null)
      throw new IllegalArgumentException("path in TreePath must be non null.");
    parentPath = parent;
    lastPathComponent = lastElement;
  }

  /**
   * Constructs a new TreePath with the identified path components of
   * length <code>length</code>.
   */
  protected TreePath(List<TreePathComponent> path, int length) {
    lastPathComponent = path.get(length - 1);
    if (length > 1)
      parentPath = new TreePath(path, length - 1);
  }

  /**
   * Returns an ordered list of TreePathComponents containing the components of this
   * TreePath. The first element (index 0) is the root.
   *
   * @return an array of TreePathComponents representing the TreePath
   */
  public List<TreePathComponent> getPath() {
    List<TreePathComponent> result = new ArrayList<TreePathComponent>();

    for (TreePath path = this; path != null; path = path.parentPath) {
      result.add(path.lastPathComponent);
    }
    Collections.reverse(result);
    return result;
  }

  /**
   * Returns the last component of this path. For a path returned by
   * DefaultTreeModel this will return an instance of TreeNode.
   *
   * @return the Object at the end of the path
   */
  public TreePathComponent getLastPathComponent() {
    return lastPathComponent;
  }

  /**
   * Returns the number of elements in the path.
   *
   * @return an int giving a count of items the path
   */
  public int getPathCount() {
    int result = 0;
    for (TreePath path = this; path != null; path = path.parentPath) {
      result++;
    }
    return result;
  }

  /**
   * Returns the path component at the specified index.
   *
   * @param element an int specifying an element in the path, where
   *                0 is the first element in the path
   * @return the Object at that index location
   * @throws IllegalArgumentException if the index is beyond the length
   *                                  of the path
   */
  public TreePathComponent getPathComponent(int element) {
    int pathLength = getPathCount();

    if (element < 0 || element >= pathLength)
      throw new IllegalArgumentException("Index " + element + " is out of the specified range");

    TreePath path = this;

    for (int i = pathLength - 1; i != element; i--) {
      path = path.parentPath;
    }
    return path.lastPathComponent;
  }

  /**
   * Tests two TreePaths for equality by checking each element of the
   * paths for equality. Two paths are considered equal if they are of
   * the same length, and contain
   * the same elements (<code>.equals</code>).
   *
   * @param o the Object to compare
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (o instanceof TreePath) {
      TreePath oTreePath = (TreePath) o;

      if (getPathCount() != oTreePath.getPathCount())
        return false;
      for (TreePath path = this; path != null; path = path.parentPath) {
        if (!(path.lastPathComponent.equals
        (oTreePath.lastPathComponent))) {
          return false;
        }
        oTreePath = oTreePath.parentPath;
      }
      return true;
    }
    return false;
  }

  /**
   * Returns the hashCode for the object. The hash code of a TreePath
   * is defined to be the hash code of the last component in the path.
   *
   * @return the hashCode for the object
   */
  public int hashCode() {
    return lastPathComponent.hashCode();
  }

  /**
   * Returns true if <code>aTreePath</code> is a
   * descendant of this
   * TreePath. A TreePath P1 is a descendent of a TreePath P2
   * if P1 contains all of the components that make up
   * P2's path.
   * For example, if this object has the path [a, b],
   * and <code>aTreePath</code> has the path [a, b, c],
   * then <code>aTreePath</code> is a descendant of this object.
   * However, if <code>aTreePath</code> has the path [a],
   * then it is not a descendant of this object.
   *
   * @return true if <code>aTreePath</code> is a descendant of this path
   */
  public boolean isDescendant(TreePath aTreePath) {
    if (aTreePath == this)
      return true;

    if (aTreePath != null) {
      int pathLength = getPathCount();
      int oPathLength = aTreePath.getPathCount();

      if (oPathLength < pathLength)
        // Can't be a descendant, has fewer components in the path.
        return false;
      while (oPathLength-- > pathLength)
        aTreePath = aTreePath.getParentPath();
      return equals(aTreePath);
    }
    return false;
  }

  /**
   * Returns a new path containing all the elements of this object
   * plus <code>child</code>. <code>child</code> will be the last element
   * of the newly created TreePath.
   * This will throw a NullPointerException
   * if child is null.
   */
  public TreePath pathByAddingChild(TreePathComponent child) {
    if (child == null)
      throw new NullPointerException("Null child not allowed");

    return new TreePath(this, child);
  }

  /**
   * Returns a path containing all the elements of this object, except
   * the last path component.
   */
  public TreePath getParentPath() {
    return parentPath;
  }

  /**
   * Returns a string that displays and identifies this
   * object's properties.
   *
   * @return a String representation of this object
   */
  public String toString() {
    StringBuffer tempSpot = new StringBuffer("[");

    for (int counter = 0, maxCounter = getPathCount(); counter < maxCounter;
         counter++) {
      if (counter > 0)
        tempSpot.append(", ");
      tempSpot.append(getPathComponent(counter));
    }
    tempSpot.append("]");
    return tempSpot.toString();
  }
}
