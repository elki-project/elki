package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex
 */
public class MetricalIndexRangeQuery<O, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, D, ?, ?> index;

  /**
   * Constructor.
   * 
   * @param relation Relation to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MetricalIndexRangeQuery(Relation<? extends O> relation, AbstractMTree<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(relation, distanceQuery);
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
            AbstractMTreeNode<O, D, ?, ?> child = index.getNode(entry.getEntryID());
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
            AbstractMTreeNode<O, D, ?, ?> child = index.getNode(entry.getEntryID());
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