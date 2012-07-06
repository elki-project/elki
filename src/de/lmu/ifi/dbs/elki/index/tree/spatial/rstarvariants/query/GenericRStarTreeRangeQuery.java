package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.GenericDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
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
public class GenericRStarTreeRangeQuery<O extends SpatialComparable, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?> tree;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param distanceQuery Distance query to use
   */
  public GenericRStarTreeRangeQuery(AbstractRStarTree<?, ?> tree, SpatialDistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
    this.tree = tree;
    this.distanceFunction = distanceQuery.getDistanceFunction();
  }

  /**
   * Perform the actual query process.
   * 
   * @param object Query object
   * @param epsilon Query range
   * @return Objects contained in query range.
   */
  protected DistanceDBIDResult<D> doRangeQuery(O object, D epsilon) {
    final GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();
    final Heap<GenericDistanceSearchCandidate<D>> pq = new Heap<GenericDistanceSearchCandidate<D>>();

    // push root
    pq.add(new GenericDistanceSearchCandidate<D>(distanceFunction.getDistanceFactory().nullDistance(), tree.getRootID()));

    // search in tree
    while(!pq.isEmpty()) {
      GenericDistanceSearchCandidate<D> pqNode = pq.poll();
      if(pqNode.mindist.compareTo(epsilon) > 0) {
        break;
      }

      AbstractRStarTreeNode<?, ?> node = tree.getNode(pqNode.nodeID);
      final int numEntries = node.getNumEntries();

      for(int i = 0; i < numEntries; i++) {
        D distance = distanceFunction.minDist(node.getEntry(i), object);
        if(distance.compareTo(epsilon) <= 0) {
          if(node.isLeaf()) {
            LeafEntry entry = (LeafEntry) node.getEntry(i);
            result.add(distance, entry.getDBID());
          }
          else {
            DirectoryEntry entry = (DirectoryEntry) node.getEntry(i);
            pq.add(new GenericDistanceSearchCandidate<D>(distance, entry.getEntryID()));
          }
        }
      }
    }

    // sort the result according to the distances
    result.sort();
    return result;
  }

  @Override
  public DistanceDBIDResult<D> getRangeForObject(O obj, D range) {
    return doRangeQuery(obj, range);
  }

  @Override
  public DistanceDBIDResult<D> getRangeForDBID(DBIDRef id, D range) {
    return getRangeForObject(relation.get(id), range);
  }
}