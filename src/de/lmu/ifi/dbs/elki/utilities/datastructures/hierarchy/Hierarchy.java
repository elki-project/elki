package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
 * This interface represents an (external) hierarchy of objects. It can contain
 * arbitrary objects, BUT the hierarchy has to be accessed using the hierarchy
 * object, i.e. {@code hierarchy.iterChildren(object);}.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Iter
 * 
 * @param <O> Object type
 */
public interface Hierarchy<O> {
  /**
   * Total size - number of objects contained.
   * 
   * @return Size
   */
  int size();

  /**
   * Get number of children
   * 
   * @param self object to get number of children for
   * @return number of children
   */
  int numChildren(O self);

  /**
   * Iterate over the (direct) children.
   * 
   * @param self object to get children for
   * @return iterator for children
   */
  Iter<O> iterChildren(O self);

  /**
   * Iterate descendants (recursive children)
   * 
   * @param self object to get descendants for
   * @return iterator for descendants
   */
  Iter<O> iterDescendants(O self);

  /**
   * Get number of (direct) parents
   * 
   * @param self reference object
   * @return number of parents
   */
  int numParents(O self);

  /**
   * Iterate over the (direct) parents.
   * 
   * @param self object to get parents for
   * @return iterator of parents
   */
  Iter<O> iterParents(O self);

  /**
   * Iterate ancestors (recursive parents)
   * 
   * @param self object to get ancestors for
   * @return iterator for ancestors
   */
  Iter<O> iterAncestors(O self);

  /**
   * Iterate over all members.
   * 
   * @return Iterator over all members.
   */
  Iter<O> iterAll();

  /**
   * Iterator interface.
   * 
   * TODO: add a skipSubtree method?
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type.
   */
  static interface Iter<O> extends de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter {
    /**
     * Access the current object.
     * 
     * @return Current object
     */
    O get();
  }
}
