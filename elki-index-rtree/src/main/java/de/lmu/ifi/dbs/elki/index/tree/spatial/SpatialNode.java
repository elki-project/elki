/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.index.tree.Node;

/**
 * Defines the requirements for an object that can be used as a node in a
 * Spatial Index. A spatial node can be a spatial directory node or a spatial
 * leaf node.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - SpatialEntry
 * 
 * @param <N> Self reference
 * @param <E> Entry type
 */
public interface SpatialNode<N extends SpatialNode<N, E>, E extends SpatialEntry> extends Node<E> {
  // No additional methods.
}