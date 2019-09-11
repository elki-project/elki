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

import elki.data.spatial.SpatialComparable;
import elki.database.ids.DBIDRef;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
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
 * @assoc - - - AbstractRStarTree
 * @assoc - - - SpatialPrimitiveDistance
 */
@Reference(authors = "J. Kuan, P. Lewis", //
    title = "Fast k nearest neighbour search for R-tree family", //
    booktitle = "Proc. Int. Conf Information, Communications and Signal Processing, ICICS 1997", //
    url = "https://doi.org/10.1109/ICICS.1997.652114", //
    bibkey = "doi:10.1109/ICICS.1997.652114")
public class RStarTreeRangeQuery<O extends SpatialComparable> implements RangeQuery<O> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?, ?> tree;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistance<? super O> distanceFunction;

  /**
   * Relation we query.
   */
  protected Relation<? extends O> relation;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Data relation to query
   * @param distanceFunction Distance function
   */
  public RStarTreeRangeQuery(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation, SpatialPrimitiveDistance<? super O> distanceFunction) {
    super();
    this.relation = relation;
    this.tree = tree;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return getRangeForObject(relation.get(id), range, result);
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
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
          double distance = distanceFunction.minDist(obj, entry);
          tree.statistics.countDistanceCalculation();
          if(distance <= range) {
            result.add(distance, entry.getDBID());
          }
        }
      }
      else {
        for(int i = 0; i < numEntries; i++) {
          SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
          double distance = distanceFunction.minDist(obj, entry);
          tree.statistics.countDistanceCalculation();
          if(distance <= range) {
            if(ps == pq.length) {
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
