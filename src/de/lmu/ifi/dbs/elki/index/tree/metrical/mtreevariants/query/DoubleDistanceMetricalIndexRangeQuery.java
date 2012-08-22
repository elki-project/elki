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
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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
public class DoubleDistanceMetricalIndexRangeQuery<O> extends AbstractDistanceRangeQuery<O, DoubleDistance> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, DoubleDistance, ?, ?> index;

  /**
   * Distance function
   */
  protected PrimitiveDoubleDistanceFunction<? super O> distf;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   * @param distf Distance function
   */
  public DoubleDistanceMetricalIndexRangeQuery(AbstractMTree<O, DoubleDistance, ?, ?> index, DistanceQuery<O, DoubleDistance> distanceQuery, PrimitiveDoubleDistanceFunction<? super O> distf) {
    super(distanceQuery);
    this.index = index;
    this.distf = distf;
  }

  /**
   * Performs a range query on the specified subtree. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   * 
   * @param id_p the routing object of the specified node
   * @param node the root of the subtree to be traversed
   * @param q the query object
   * @param r_q the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(DBID id_p, AbstractMTreeNode<O, DoubleDistance, ?, ?> node, O q, double r_q, DoubleDistanceDBIDList result) {
    final O o_p = id_p != null ? relation.get(id_p) : null;
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<DoubleDistance> entry = node.getEntry(i);
        DBID id_r = entry.getRoutingObjectID();

        double r_or = entry.getCoveringRadius().doubleValue();
        double d1 = id_p != null ? distf.doubleDistance(o_p, q) : 0;
        double d2 = id_p != null ? entry.getParentDistance().doubleValue() : 0;
        double diff = Math.abs(d1 - d2);

        double sum = r_q + r_or;

        if(diff <= sum) {
          double d3 = distf.doubleDistance(relation.get(id_r), q);
          if(d3 <= sum) {
            AbstractMTreeNode<O, DoubleDistance, ?, ?> child = index.getNode(((DirectoryEntry) entry).getPageID());
            doRangeQuery(id_r, child, q, r_q, result);
          }
        }
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<DoubleDistance> entry = node.getEntry(i);
        DBID id_j = entry.getRoutingObjectID();
        O o_j = relation.get(id_j);

        double d1 = id_p != null ? distf.doubleDistance(o_p, q) : 0;
        double d2 = id_p != null ? distf.doubleDistance(o_j, o_p) : 0;
        double diff = Math.abs(d1 - d2);

        if(diff <= r_q) {
          double d3 = distf.doubleDistance(o_j, q);
          if(d3 <= r_q) {
            result.add(d3, id_j);
          }
        }
      }
    }
  }

  @Override
  public DistanceDBIDResult<DoubleDistance> getRangeForObject(O obj, DoubleDistance range) {
    final DoubleDistanceDBIDList result = new DoubleDistanceDBIDList();

    doRangeQuery(null, index.getRoot(), obj, range.doubleValue(), result);
    result.sort();
    return result;
  }

  @Override
  public DistanceDBIDResult<DoubleDistance> getRangeForDBID(DBIDRef id, DoubleDistance range) {
    return getRangeForObject(relation.get(id), range);
  }
}