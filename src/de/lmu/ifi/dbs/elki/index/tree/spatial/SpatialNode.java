package de.lmu.ifi.dbs.elki.index.tree.spatial;
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Node;

/**
 * Defines the requirements for an object that can be used as a node in a
 * Spatial Index. A spatial node can be a spatial directory node or a spatial
 * leaf node.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf SpatialEntry
 * 
 * @param <N> Self reference
 * @param <E> Entry type
 */
public interface SpatialNode<N extends SpatialNode<N, E>, E extends SpatialEntry> extends Node<E>, SpatialComparable {
  // No additional methods.
}
