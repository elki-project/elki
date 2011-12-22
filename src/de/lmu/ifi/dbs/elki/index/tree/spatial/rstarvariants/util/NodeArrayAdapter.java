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

import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;

/**
 * Access the entries of a node as array-like.
 * 
 * @author Erich Schubert
 */
public class NodeArrayAdapter implements ArrayAdapter<SpatialEntry, AbstractNode<? extends SpatialEntry>> {
  /**
   * Static adapter.
   */
  public static NodeArrayAdapter STATIC = new NodeArrayAdapter();

  /**
   * Constructor.
   */
  protected NodeArrayAdapter() {
    super();
    // TODO Auto-generated constructor stub
  }

  @Override
  public int size(AbstractNode<? extends SpatialEntry> array) {
    return array.getNumEntries();
  }

  @Override
  public SpatialEntry get(AbstractNode<? extends SpatialEntry> array, int off) throws IndexOutOfBoundsException {
    return array.getEntry(off);
  }
}