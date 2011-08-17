package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
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
  private void doRangeQuery(DBID o_p, AbstractMTreeNode<O, D, ?, ?> node, DBID q, D r_q, List<DistanceResultPair<D>> result) {
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? entry.getParentDistance() : getDistanceFactory().nullDistance();
        // o_p != null ?  distanceFunction.distance(o_r, o_p) :/ distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if(diff.compareTo(sum) <= 0) {
          D d3 = distanceQuery.distance(o_r, q);
          if(d3.compareTo(sum) <= 0) {
            AbstractMTreeNode<O, D, ?, ?> child = index.getNode(((DirectoryEntry)entry).getPageID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }

      }
    }

    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        if(diff.compareTo(r_q) <= 0) {
          D d3 = distanceQuery.distance(o_j, q);
          if(d3.compareTo(r_q) <= 0) {
            DistanceResultPair<D> queryResult = new GenericDistanceResultPair<D>(d3, o_j);
            result.add(queryResult);
          }
        }
      }
    }
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
  private void doRangeQuery(DBID o_p, AbstractMTreeNode<O, D, ?, ?> node, O q, D r_q, List<DistanceResultPair<D>> result) {
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? entry.getParentDistance() : getDistanceFactory().nullDistance();
        // o_p != null ? distanceFunction.distance(o_r, o_p) : distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if(diff.compareTo(sum) <= 0) {
          D d3 = distanceQuery.distance(o_r, q);
          if(d3.compareTo(sum) <= 0) {
            AbstractMTreeNode<O, D, ?, ?> child = index.getNode(((DirectoryEntry)entry).getPageID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        if(diff.compareTo(r_q) <= 0) {
          D d3 = distanceQuery.distance(o_j, q);
          if(d3.compareTo(r_q) <= 0) {
            DistanceResultPair<D> queryResult = new GenericDistanceResultPair<D>(d3, o_j);
            result.add(queryResult);
          }
        }
      }
    }
  }
  
  @Override
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range) {
    final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();

    doRangeQuery(null, index.getRoot(), obj, range, result);

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    return getRangeForObject(relation.get(id), range);
  }
}