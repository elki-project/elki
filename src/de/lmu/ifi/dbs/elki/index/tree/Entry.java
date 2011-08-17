package de.lmu.ifi.dbs.elki.index.tree;
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

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in an index structure. An entry can
 * represent a node or a data object.
 * 
 * @author Elke Achtert
 */
public interface Entry extends Externalizable {
  /**
   * Returns true if this entry is an entry in a leaf node (i.e. this entry
   * represents a data object), false otherwise.
   * 
   * @return true if this entry is an entry in a leaf node, false otherwise
   */
  public boolean isLeafEntry();
}