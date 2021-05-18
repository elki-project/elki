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
package elki.index.tree.spatial.rstarvariants.query;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialPointLeafEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import elki.utilities.documentation.Reference;

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
 * @assoc - - - EuclideanDistance
 * @assoc - - - SquaredEuclideanDistance
 */
@Reference(authors = "J. Kuan, P. Lewis", //
    title = "Fast k nearest neighbour search for R-tree family", //
    booktitle = "Proc. Int. Conf Information, Communications and Signal Processing, ICICS 1997", //
    url = "https://doi.org/10.1109/ICICS.1997.652114", //
    bibkey = "doi:10.1109/ICICS.1997.652114")
public class EuclideanRStarTreeRangeQuery<O extends NumberVector> extends RStarTreeRangeSearcher<O> {
  /**
   * Squared euclidean distance function.
   */
  private static final SquaredEuclideanDistance SQUARED = SquaredEuclideanDistance.STATIC;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Relation to use.
   */
  public EuclideanRStarTreeRangeQuery(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation) {
    super(tree, relation, EuclideanDistance.STATIC);
  }

  @Override
  public ModifiableDoubleDBIDList getRange(O object, double range, ModifiableDoubleDBIDList result) {
    final SquaredEuclideanDistance squared = SQUARED;
    final double sqepsilon = range * range;
    tree.statistics.countRangeQuery();

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
          double distance = squared.minDist(object, entry);
          tree.statistics.countDistanceCalculation();
          if(distance <= sqepsilon) {
            result.add(Math.sqrt(distance), entry.getDBID());
          }
        }
      }
      else {
        for(int i = 0; i < numEntries; i++) {
          SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
          double distance = squared.minDist(object, entry);
          if(distance <= sqepsilon) {
            if(ps == pq.length) { // Resize:
              pq = Arrays.copyOf(pq, pq.length + (pq.length >>> 1));
            }
            pq[ps++] = entry.getPageID();
          }
        }
      }
    }
    return result;
  }
}
