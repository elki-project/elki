package de.lmu.ifi.dbs.elki.index.tree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a path to a node in an index structure.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf TreeIndexPathComponent
 * @apiviz.uses Entry
 * 
 * @param <E> the type of Entry used in the index
 */
public class IndexTreePath<E extends Entry> {
  /**
   * Path representing the parent, null if lastPathComponent represents the
   * root.
   */
  private IndexTreePath<E> parentPath;

  /**
   * Last path component.
   */
  private TreeIndexPathComponent<E> lastPathComponent;

  /**
   * Constructs a path from a list of path components, uniquely identifying the
   * path from the root of the index to a specific node. The first element in
   * the path is the root of the index, the last node is the node identified by
   * the path.
   * 
   * @param path a list of IndexPathComponents representing the path to a node
   */
  public IndexTreePath(List<TreeIndexPathComponent<E>> path) {
    if(path == null || path.size() == 0) {
      throw new IllegalArgumentException("Path in IndexPath must be non null and not empty.");
    }
    lastPathComponent = path.get(path.size() - 1);
    if(path.size() > 1) {
      parentPath = new IndexTreePath<E>(path, path.size() - 1);
    }
  }

  /**
   * Constructs a IndexPath containing only a single element. This is usually
   * used to construct a IndexPath for the the root of the index.
   * 
   * @param singlePath a IndexPathComponent representing the path to a node
   */
  public IndexTreePath(TreeIndexPathComponent<E> singlePath) {
    if(singlePath == null) {
      throw new IllegalArgumentException("path in TreePath must be non null.");
    }
    lastPathComponent = singlePath;
    parentPath = null;
  }

  /**
   * Constructs a new IndexPath, which is the path identified by
   * <code>parent</code> ending in <code>lastElement</code>.
   * 
   * @param parent the parent path
   * @param lastElement the last path component
   */
  protected IndexTreePath(IndexTreePath<E> parent, TreeIndexPathComponent<E> lastElement) {
    if(lastElement == null) {
      throw new IllegalArgumentException("path in TreePath must be non null.");
    }
    parentPath = parent;
    lastPathComponent = lastElement;
  }

  /**
   * Constructs a new IndexPath with the identified path components of length
   * <code>length</code>.
   * 
   * @param path the whole path
   * @param length the length of the newly created index path
   */
  protected IndexTreePath(List<TreeIndexPathComponent<E>> path, int length) {
    lastPathComponent = path.get(length - 1);
    if(length > 1) {
      parentPath = new IndexTreePath<E>(path, length - 1);
    }
  }

  /**
   * Returns an ordered list of IndexPathComponents containing the components of
   * this IndexPath. The first element (index 0) is the root.
   * 
   * @return an array of IndexPathComponent representing the IndexPath
   */
  public List<TreeIndexPathComponent<E>> getPath() {
    List<TreeIndexPathComponent<E>> result = new ArrayList<TreeIndexPathComponent<E>>();

    for(IndexTreePath<E> path = this; path != null; path = path.parentPath) {
      result.add(path.lastPathComponent);
    }
    Collections.reverse(result);
    return result;
  }

  /**
   * Returns the last component of this path.
   * 
   * @return the IndexPathComponent at the end of the path
   */
  public TreeIndexPathComponent<E> getLastPathComponent() {
    return lastPathComponent;
  }

  /**
   * Returns the number of elements in the path.
   * 
   * @return an int giving a count of items the path
   */
  public int getPathCount() {
    int result = 0;
    for(IndexTreePath<E> path = this; path != null; path = path.parentPath) {
      result++;
    }
    return result;
  }

  /**
   * Returns the path component at the specified index.
   * 
   * @param element an int specifying an element in the path, where 0 is the
   *        first element in the path
   * @return the Object at that index location
   * @throws IllegalArgumentException if the index is beyond the length of the
   *         path
   */
  public TreeIndexPathComponent<E> getPathComponent(int element) {
    int pathLength = getPathCount();

    if(element < 0 || element >= pathLength) {
      throw new IllegalArgumentException("Index " + element + " is out of the specified range");
    }

    IndexTreePath<E> path = this;

    for(int i = pathLength - 1; i != element; i--) {
      path = path.parentPath;
    }
    return path.lastPathComponent;
  }

  /**
   * Returns <code>true</code> if <code>this == o</code> has the value
   * <code>true</code> or o is not null and o is of the same class as this
   * instance and the two index paths are of the same length, and contain the
   * same components (<code>.equals</code>), <code>false</code> otherwise.
   * 
   * @see de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent#equals(Object)
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    IndexTreePath<E> other = (IndexTreePath<E>) o;

    if(getPathCount() != other.getPathCount()) {
      return false;
    }
    for(IndexTreePath<E> path = this; path != null; path = path.parentPath) {
      if(!(path.lastPathComponent.equals(other.lastPathComponent))) {
        return false;
      }
      other = other.parentPath;
    }
    return true;
  }

  /**
   * Returns the hash code for this index path. The hash code of a TreeIndexPath
   * is defined to be the hash code of the last component in the path.
   * 
   * @return the hash code of the last component in the index path
   */
  @Override
  public int hashCode() {
    return lastPathComponent.hashCode();
  }

  /**
   * Returns true if <code>aIndexPath</code> is a descendant of this IndexPath.
   * A IndexPath P1 is a descendent of a IndexPath P2 if P1 contains all of the
   * components that make up P2's path. For example, if this object has the path
   * [a, b], and <code>aIndexPath</code> has the path [a, b, c], then
   * <code>aIndexPath</code> is a descendant of this object. However, if
   * <code>aIndexPath</code> has the path [a], then it is not a descendant of
   * this object.
   * 
   * @param aIndexPath the index path to be tested
   * @return true if <code>aIndexPath</code> is a descendant of this path
   */
  public boolean isDescendant(IndexTreePath<E> aIndexPath) {
    if(aIndexPath == this) {
      return true;
    }

    if(aIndexPath != null) {
      int pathLength = getPathCount();
      int oPathLength = aIndexPath.getPathCount();

      if(oPathLength < pathLength) {
        // Can't be a descendant, has fewer components in the path.
        return false;
      }
      while(oPathLength-- > pathLength) {
        aIndexPath = aIndexPath.getParentPath();
      }
      return equals(aIndexPath);
    }
    return false;
  }

  /**
   * Returns a new path containing all the elements of this object plus
   * <code>child</code>. <code>child</code> will be the last element of the
   * newly created IndexPath. This will throw a NullPointerException if child is
   * null.
   * 
   * @param child the last element of the newly created IndexPath
   * @return a new path containing all the elements of this object plus
   *         <code>child</code>
   */
  public IndexTreePath<E> pathByAddingChild(TreeIndexPathComponent<E> child) {
    if(child == null) {
      throw new NullPointerException("Null child not allowed");
    }

    return new IndexTreePath<E>(this, child);
  }

  /**
   * Returns a path containing all the elements of this object, except the last
   * path component.
   * 
   * @return a path containing all the elements of this object, except the last
   *         path component
   */
  public IndexTreePath<E> getParentPath() {
    return parentPath;
  }

  /**
   * Returns a string that displays the components of this index path.
   * 
   * @return a string representation of the components of this index path
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("[");

    for(int counter = 0, maxCounter = getPathCount(); counter < maxCounter; counter++) {
      if(counter > 0) {
        buffer.append(", ");
      }
      buffer.append(getPathComponent(counter));
    }
    buffer.append("]");
    return buffer.toString();
  }
}
