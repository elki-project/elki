package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractMTree
 */
public class MetricalIndexRangeQuery<O, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, D, ?, ?> index;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MetricalIndexRangeQuery(AbstractMTree<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
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
   * @param q the id of the query object
   * @param r_q the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(DBID o_p, AbstractMTreeNode<O, D, ?, ?> node, O q, D r_q, GenericDistanceDBIDList<D> result) {
    final D nullDistance = distanceQuery.nullDistance();
    D d1 = o_p != null ? distanceQuery.distance(o_p, q) : nullDistance;
    if (!node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d2 = o_p != null ? entry.getParentDistance() : nullDistance;
        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if (diff.compareTo(sum) <= 0) {
          D d3 = distanceQuery.distance(o_r, q);
          if (d3.compareTo(sum) <= 0) {
            AbstractMTreeNode<O, D, ?, ?> child = index.getNode(((DirectoryEntry) entry).getPageID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }
      }
    } else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        D d2 = o_p != null ? entry.getParentDistance() : nullDistance;

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        if (diff.compareTo(r_q) <= 0) {
          D d3 = distanceQuery.distance(o_j, q);
          if (d3.compareTo(r_q) <= 0) {
            result.add(d3, o_j);
          }
        }
      }
    }
  }

  @Override
  public DistanceDBIDResult<D> getRangeForObject(O obj, D range) {
    final GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();

    doRangeQuery(null, index.getRoot(), obj, range, result);

    // sort the result according to the distances
    result.sort();
    return result;
  }

  @Override
  public DistanceDBIDResult<D> getRangeForDBID(DBIDRef id, D range) {
    return getRangeForObject(relation.get(id), range);
  }
}
