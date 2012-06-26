package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

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

import java.util.Iterator;
import java.util.List;

/**
 * This interface represents an (external) hierarchy of objects. It can contain
 * arbitrary objects, BUT the hierarchy has to be accessed using the hierarchy
 * object, i.e. {@code hierarchy.getChildren(object);}.
 * 
 * See {@link Hierarchical} for an interface for objects with an internal
 * hierarchy (where you can use {@code object.getChildren();})
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface Hierarchy<O> {
  /**
   * Get number of children
   * 
   * @param self object to get number of children for
   * @return number of children
   */
  public int numChildren(O self);

  /**
   * Get children list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @param self object to get children for
   * @return list of children
   */
  public List<O> getChildren(O self);

  /**
   * Iterate descendants (recursive children)
   * 
   * @param self object to get descendants for
   * @return iterator for descendants
   */
  public Iterator<O> iterDescendants(O self);

  /**
   * Get number of (direct) parents
   * 
   * @param self reference object
   * @return number of parents
   */
  public int numParents(O self);

  /**
   * Get parents list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @param self object to get parents for
   * @return list of parents
   */
  public List<O> getParents(O self);

  /**
   * Iterate ancestors (recursive parents)
   * 
   * @param self object to get ancestors for
   * @return iterator for ancestors
   */
  public Iterator<O> iterAncestors(O self);
}