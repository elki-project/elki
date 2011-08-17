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


/**
 * Modifiable Hierarchy.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface ModifiableHierarchy<O> extends Hierarchy<O> {
  /**
   * Add a parent-child relationship.
   * 
   * @param parent Parent
   * @param child Child
   */
  // TODO: return true when new?
  public void add(O parent, O child);

  /**
   * Remove a parent-child relationship.
   * 
   * @param parent Parent
   * @param child Child
   */
  // TODO: return true when found?
  public void remove(O parent, O child);
}
