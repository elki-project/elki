package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;


/**
 * Interface for objects with an <b>internal</b> hierarchy interface.
 * 
 * Note that the object can chose to delegate the hierarchy to an external hierarchy.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type in hierarchy
 */
public interface Hierarchical<O> {
  /**
   * Test for hierarchical properties
   * 
   * @return hierarchical data model.
   */
  public boolean isHierarchical();

  /**
   * Get number of children
   * 
   * @return number of children
   */
  public int numChildren();

  /**
   * Get children list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @return list of children
   */
  public List<O> getChildren();

  /**
   * Iterate descendants (recursive children)
   * 
   * @return iterator for descendants
   */
  public IterableIterator<O> iterDescendants();
  
  /**
   * Get number of parents
   * 
   * @return number of parents
   */
  public int numParents();

  /**
   * Get parents list. Resulting list MAY be modified. Result MAY be null, if
   * the model is not hierarchical.
   * 
   * @return list of parents
   */
  public List<O> getParents();

  /**
   * Iterate ancestors (recursive parents)
   * 
   * @return iterator for ancestors
   */
  public IterableIterator<O> iterAncestors();
}