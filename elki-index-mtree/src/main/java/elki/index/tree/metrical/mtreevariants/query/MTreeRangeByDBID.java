/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.metrical.mtreevariants.query;

import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.range.RangeSearcher;
import elki.index.tree.DirectoryEntry;
import elki.index.tree.metrical.mtreevariants.AbstractMTree;
import elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - AbstractMTree
 * 
 * @param <O> Object type
 */
public class MTreeRangeByDBID<O> implements RangeSearcher<DBIDRef> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, ?, ?, ?> index;

  /**
   * Hold the distance function to be used.
   */
  protected final DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MTreeRangeByDBID(AbstractMTree<O, ?, ?, ?> index, DistanceQuery<O> distanceQuery) {
    super();
    this.index = index;
    this.distanceQuery = distanceQuery;
  }

  /**
   * Performs a range query on the specified subtree. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   * 
   * @param o_p the routing object of the specified node
   * @param node the root of the subtree to be traversed
   * @param q the query object
   * @param r_q the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(DBID o_p, AbstractMTreeNode<O, ?, ?> node, DBIDRef q, double r_q, ModifiableDoubleDBIDList result) {
    double d1 = 0.;
    if(o_p != null) {
      d1 = distanceQuery.distance(o_p, q);
      index.statistics.countDistanceCalculation();
    }
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry entry = node.getEntry(i);
        double d2 = o_p != null ? entry.getParentDistance() : 0.;
        double sum = r_q + entry.getCoveringRadius();
        if(Math.abs(d1 - d2) <= sum) {
          index.statistics.countDistanceCalculation();
          DBID o_r = entry.getRoutingObjectID();
          if(distanceQuery.distance(o_r, q) <= sum) {
            doRangeQuery(o_r, index.getNode(((DirectoryEntry) entry).getPageID()), q, r_q, result);
          }
        }
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry entry = node.getEntry(i);
        double d2 = o_p != null ? entry.getParentDistance() : 0.;
        if(Math.abs(d1 - d2) <= r_q) {
          DBID o_j = entry.getRoutingObjectID();
          double d3 = distanceQuery.distance(o_j, q);
          index.statistics.countDistanceCalculation();
          if(d3 <= r_q) {
            result.add(d3, o_j);
          }
        }
      }
    }
  }

  @Override
  public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
    index.statistics.countRangeQuery();
    doRangeQuery(null, index.getNode(index.getRootID()), query, range, result);
    return result;
  }
}
