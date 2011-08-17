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

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.Node;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;

/**
 * Insertion strategy that exhaustively tests all childs for the least overlap
 * when inserting.
 * 
 * @author Elke Achtert
 */
public class LeastOverlapInsertionStrategy implements InsertionStrategy {
  /**
   * Constructor.
   */
  public LeastOverlapInsertionStrategy() {
    super();
  }

  /**
   * Returns the path information of the entry of the specified node which needs
   * least overlap enlargement if the given mbr would be inserted into.
   * 
   * @param node the node of which the children should be tested
   * @param mbr the mbr to be inserted into the children
   * @return the path information of the entry which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  @Override
  public <N extends Node<E>, E extends SpatialEntry> TreeIndexPathComponent<E> findInsertChild(N node, SpatialComparable mbr) {
    Enlargement<E> min = null;

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry_i = node.getEntry(i);
      HyperBoundingBox newMBR = SpatialUtil.unionTolerant(mbr, entry_i);

      double currOverlap = 0;
      double newOverlap = 0;
      for(int k = 0; k < node.getNumEntries(); k++) {
        if(i != k) {
          E entry_k = node.getEntry(k);
          currOverlap += SpatialUtil.relativeOverlap(entry_i, entry_k);
          newOverlap += SpatialUtil.relativeOverlap(newMBR, entry_k);
        }
      }

      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement<E> enlargement = new Enlargement<E>(new TreeIndexPathComponent<E>(entry_i, i), volume, inc_volume, inc_overlap);

      if(min == null || min.compareTo(enlargement) > 0) {
        min = enlargement;
      }
    }

    assert min != null;
    return min.getPathComponent();
  }
}
