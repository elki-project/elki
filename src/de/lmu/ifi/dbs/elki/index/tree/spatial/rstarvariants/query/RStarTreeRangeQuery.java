package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * Reference:
 * <p>
 * J. Kuan, P. Lewis<br />
 * Fast k nearest neighbour search for R-tree family<br />
 * In Proc. Int. Conf Information, Communications and Signal Processing, ICICS
 * 1997
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 */
@Reference(authors = "J. Kuan, P. Lewis", title = "Fast k nearest neighbour search for R-tree family", booktitle = "Proc. Int. Conf Information, Communications and Signal Processing, ICICS 1997", url = "http://dx.doi.org/10.1109/ICICS.1997.652114")
public class RStarTreeRangeQuery<O extends SpatialComparable> extends AbstractDistanceRangeQuery<O> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?, ?> tree;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param distanceQuery Distance query to use
   * @param distanceFunction Distance function
   */
  public RStarTreeRangeQuery(AbstractRStarTree<?, ?, ?> tree, DistanceQuery<O> distanceQuery, SpatialPrimitiveDistanceFunction<? super O> distanceFunction) {
    super(distanceQuery);
    this.tree = tree;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Perform the actual query process.
   * 
   * @param object Query object
   * @param epsilon Query range
   * @return Objects contained in query range.
   */
  protected DoubleDBIDList doRangeQuery(O object, double epsilon) {
    tree.statistics.countRangeQuery();
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();

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
          double distance = distanceFunction.minDist(object, entry);
          tree.statistics.countDistanceCalculation();
          if(distance <= epsilon) {
            result.add(distance, entry.getDBID());
          }
        }
      }
      else {
        for(int i = 0; i < numEntries; i++) {
          SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
          double distance = distanceFunction.minDist(object, entry);
          if(distance <= epsilon) {
            if(ps == pq.length) {
              pq = Arrays.copyOf(pq, pq.length + (pq.length >>> 1));
            }
            pq[ps++] = entry.getEntryID();
          }
        }
      }
    }

    // sort the result according to the distances
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    return doRangeQuery(obj, range);
  }
}