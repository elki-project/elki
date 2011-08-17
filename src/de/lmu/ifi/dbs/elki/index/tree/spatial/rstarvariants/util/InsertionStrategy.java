package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;
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
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for implementing insertion strategies, i.e. in which path of the
 * tree to insert the new element.
 * 
 * @author Erich Schubert
 */
public interface InsertionStrategy extends Parameterizable {
  /**
   * Find the child to insert into.
   * 
   * @param <N> Node type
   * @param <E> Entry type
   * @param node Node to use
   * @param mbr MBR of entry to insert
   * @return Entry to insert into
   */
  public <N extends Node<E>, E extends SpatialEntry> TreeIndexPathComponent<E> findInsertChild(N node, SpatialComparable mbr);
}
