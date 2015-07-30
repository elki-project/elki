package de.lmu.ifi.dbs.elki.index.tree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
   * The entry of this component.
   */
  private final E entry;

  /**
   * The index of this component in its parent.
   */
  private final int index;

  /**
   * Constructs a new IndexPath.
   * 
   * @param parent the parent path
   * @param entry the entry of the component
   * @param index index of the component in its parent
   */
  public IndexTreePath(IndexTreePath<E> parent, E entry, int index) {
    if(entry == null) {
      throw new IllegalArgumentException("entry in TreePath must be non null.");
    }
    this.entry = entry;
    this.index = index;
    this.parentPath = parent;
  }

  /**
   * Returns the entry of the component.
   * 
   * @return the entry of the component
   */
  public E getEntry() {
    return entry;
  }

  /**
   * Returns the index of the component in its parent.
   * 
   * @return the index of the component in its parent
   */
  public int getIndex() {
    return index;
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
  public IndexTreePath<E> getPathComponent(int element) {
    int pathLength = getPathCount();

    if(element < 0 || element >= pathLength) {
      throw new IllegalArgumentException("Index " + element + " is out of the specified range");
    }

    IndexTreePath<E> path = this;
    for(int i = pathLength - 1; i != element; i--) {
      path = path.parentPath;
    }
    return path;
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
      if(path.index != other.index || !(path.entry.equals(other.entry))) {
        return false;
      }
      other = other.parentPath;
    }
    return true;
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
