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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Instance of a range query for a particular spatial index.
 * <p>
 * Reference:
 * <p>
 * J. Kuan, P. Lewis<br>
 * Fast k nearest neighbour search for R-tree family<br>
 * Proc. Int. Conf Information, Communications and Signal Processing, ICICS 1997
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - EuclideanDistanceFunction
 * @assoc - - - SquaredEuclideanDistanceFunction
 */
@Reference(authors = "J. Kuan, P. Lewis", //
    title = "Fast k nearest neighbour search for R-tree family", //
    booktitle = "Proc. Int. Conf Information, Communications and Signal Processing, ICICS 1997", //
    url = "https://doi.org/10.1109/ICICS.1997.652114", //
    bibkey = "doi:10.1109/ICICS.1997.652114")
public class EuclideanRStarTreeRangeQuery<O extends NumberVector> extends RStarTreeRangeQuery<O> {
  /**
   * Squared euclidean distance function.
   */
  private static final SquaredEuclideanDistanceFunction SQUARED = SquaredEuclideanDistanceFunction.STATIC;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Relation to use.
   */
  public EuclideanRStarTreeRangeQuery(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation) {
    super(tree, relation, EuclideanDistanceFunction.STATIC);
  }

  @Override
  public void getRangeForObject(O object, double range, ModifiableDoubleDBIDList result) {
    tree.statistics.countRangeQuery();
    final double sqepsilon = range * range;

    // Processing queue.
    int[] pq = new int[101];
    int ps = 0;
    pq[ps++] = tree.getRootID();

    // search in tree
    while(ps > 0) {
      int pqNode = pq[--ps]; // Pop last.
      AbstractRStarTreeNode<?, ?> node = tree.getNode(pqNode);
      final int numEntries = node.getNumEntries();

      if(node.isLeaf()) {
        for(int i = 0; i < numEntries; i++) {
          SpatialPointLeafEntry entry = (SpatialPointLeafEntry) node.getEntry(i);
          double distance = SQUARED.minDist(object, entry);
          tree.statistics.countDistanceCalculation();
          if(distance <= sqepsilon) {
            result.add(FastMath.sqrt(distance), entry.getDBID());
          }
        }
      }
      else {
        for(int i = 0; i < numEntries; i++) {
          SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
          double distance = SQUARED.minDist(object, entry);
          if(distance <= sqepsilon) {
            if(ps == pq.length) { // Resize:
              pq = Arrays.copyOf(pq, pq.length + (pq.length >>> 1));
            }
            pq[ps++] = entry.getPageID();
          }
        }
      }
    }
  }
}
