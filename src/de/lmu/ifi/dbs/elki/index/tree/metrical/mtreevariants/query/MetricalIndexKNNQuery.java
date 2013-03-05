package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.GenericMTreeDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractMTree
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MetricalIndexKNNQuery<O, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
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
  public MetricalIndexKNNQuery(AbstractMTree<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
    this.index = index;
  }

  @Override
  public KNNList<D> getKNNForObject(O q, int k) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }
    index.statistics.countKNNQuery();

    final D nullDistance = index.getDistanceFactory().nullDistance();
    KNNHeap<D> knnList = DBIDUtil.newHeap(distanceQuery.getDistanceFactory(), k);
    D d_k = knnList.getKNNDistance();

    final ComparableMinHeap<GenericMTreeDistanceSearchCandidate<D>> pq = new ComparableMinHeap<>();

    // push root
    pq.add(new GenericMTreeDistanceSearchCandidate<>(nullDistance, index.getRootID(), null, nullDistance));

    // search in tree
    while (!pq.isEmpty()) {
      GenericMTreeDistanceSearchCandidate<D> pqNode = pq.poll();

      if (knnList.size() >= k && pqNode.mindist.compareTo(d_k) > 0) {
        break;
      }

      AbstractMTreeNode<?, D, ?, ?> node = index.getNode(pqNode.nodeID);
      DBID id_p = pqNode.routingObjectID;
      D d1 = pqNode.routingDistance;

      // directory node
      if (!node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MTreeEntry<D> entry = node.getEntry(i);
          DBID o_r = entry.getRoutingObjectID();
          D r_or = entry.getCoveringRadius();
          D d2 = id_p != null ? entry.getParentDistance() : nullDistance;

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if (diff.compareTo(sum) <= 0) {
            D d3 = distanceQuery.distance(o_r, q);
            index.statistics.countDistanceCalculation();
            D d_min = DistanceUtil.max(d3.minus(r_or), index.getDistanceFactory().nullDistance());
            if (d_min.compareTo(d_k) <= 0) {
              pq.add(new GenericMTreeDistanceSearchCandidate<>(d_min, ((DirectoryEntry) entry).getPageID(), o_r, d3));
            }
          }
        }
      }
      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MTreeEntry<D> entry = node.getEntry(i);
          DBID o_j = entry.getRoutingObjectID();

          D d2 = id_p != null ? entry.getParentDistance() : nullDistance;

          D diff = (d1.compareTo(d2) > 0) ? d1.minus(d2) : d2.minus(d1);

          if (diff.compareTo(d_k) <= 0) {
            D d3 = distanceQuery.distance(o_j, q);
            index.statistics.countDistanceCalculation();
            if (d3.compareTo(d_k) <= 0) {
              knnList.add(d3, o_j);
              d_k = knnList.getKNNDistance();
            }
          }
        }
      }
    }
    return knnList.toKNNList();
  }
}
