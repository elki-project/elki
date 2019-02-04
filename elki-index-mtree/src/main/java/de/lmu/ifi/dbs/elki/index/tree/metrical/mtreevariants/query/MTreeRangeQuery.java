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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

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
public class MTreeRangeQuery<O> extends AbstractDistanceRangeQuery<O> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, ?, ?, ?> index;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MTreeRangeQuery(AbstractMTree<O, ?, ?, ?> index, DistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    this.index = index;
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
  private void doRangeQuery(DBID o_p, AbstractMTreeNode<O, ?, ?> node, O q, double r_q, ModifiableDoubleDBIDList result) {
    double d1 = 0.;
    if(o_p != null) {
      d1 = distanceQuery.distance(o_p, q);
      index.statistics.countDistanceCalculation();
    }
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        double r_or = entry.getCoveringRadius();
        double d2 = o_p != null ? entry.getParentDistance() : 0.;
        double diff = Math.abs(d1 - d2);

        double sum = r_q + r_or;

        if(diff <= sum) {
          double d3 = distanceQuery.distance(o_r, q);
          index.statistics.countDistanceCalculation();
          if(d3 <= sum) {
            AbstractMTreeNode<O, ?, ?> child = index.getNode(((DirectoryEntry) entry).getPageID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        double d2 = o_p != null ? entry.getParentDistance() : 0.;

        double diff = Math.abs(d1 - d2);

        if(diff <= r_q) {
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
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    index.statistics.countRangeQuery();
    doRangeQuery(null, index.getRoot(), obj, range, result);
  }
}
