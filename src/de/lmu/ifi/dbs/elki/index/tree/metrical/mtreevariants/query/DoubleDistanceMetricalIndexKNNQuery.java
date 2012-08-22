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

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.DoubleMTreeDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractMTree
 * 
 * @param <O> Object type
 */
public class DoubleDistanceMetricalIndexKNNQuery<O> extends AbstractDistanceKNNQuery<O, DoubleDistance> {
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
  public DoubleDistanceMetricalIndexKNNQuery(AbstractMTree<O, DoubleDistance, ?, ?> index, DistanceQuery<O, DoubleDistance> distanceQuery, PrimitiveDoubleDistanceFunction<? super O> distf) {
    super(distanceQuery);
    this.index = index;
    this.distf = distf;
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForObject(O q, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }

    DoubleDistanceKNNHeap knnList = new DoubleDistanceKNNHeap(k);
    double d_k = Double.POSITIVE_INFINITY;
    
    final Heap<DoubleMTreeDistanceSearchCandidate> pq = new Heap<DoubleMTreeDistanceSearchCandidate>();

    // push root
    pq.add(new DoubleMTreeDistanceSearchCandidate(0, index.getRootID(), null));
    
    // search in tree
    while(!pq.isEmpty()) {
      DoubleMTreeDistanceSearchCandidate pqNode = pq.poll();

      if(knnList.size() >= k && pqNode.mindist > d_k) {
        break;
      }

      AbstractMTreeNode<?, DoubleDistance, ?, ?> node = index.getNode(pqNode.nodeID);
      final DBID id_p = pqNode.routingObjectID;
      final O ob_p = relation.get(id_p);

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          final MTreeEntry<DoubleDistance> entry = node.getEntry(i);
          final DBID id_i = entry.getRoutingObjectID();
          final O ob_i = relation.get(id_i);
          final double or_i = entry.getCoveringRadius().doubleValue();
          final double d1 = id_p != null ? distf.doubleDistance(ob_p, q) : 0;
          final double d2 = id_p != null ? distf.doubleDistance(ob_i, ob_p) : 0;
          final double diff = Math.abs(d1 - d2);

          if(diff <= d_k + or_i) {
            double d3 = distf.doubleDistance(ob_i, q);
            double d_min = Math.max(d3 - or_i, 0);
            if(d_min <= d_k) {
              pq.add(new DoubleMTreeDistanceSearchCandidate(d_min, ((DirectoryEntry)entry).getPageID(), id_i));
            }
          }
        }
      }
      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          final MTreeEntry<DoubleDistance> entry = node.getEntry(i);
          final DBID id_i = entry.getRoutingObjectID();
          final O o_i = relation.get(id_i);

          final double d1 = id_p != null ? distf.doubleDistance(ob_p, q) : 0;
          final double d2 = id_p != null ? distf.doubleDistance(o_i, ob_p) : 0;
          final double diff = Math.abs(d1 - d2);

          if(diff <= d_k) {
            double d3 = distf.doubleDistance(o_i, q);
            if(d3 <= d_k) {
              knnList.add(d3, id_i);
              d_k = knnList.doubleKNNDistance();
            }
          }
        }
      }
    }
    return knnList.toKNNList();
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public List<KNNResult<DoubleDistance>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}