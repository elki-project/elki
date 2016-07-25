package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.DoubleDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * Reference:
 * <p>
 * G. R. Hjaltason, H. Samet<br />
 * Ranking in spatial databases<br />
 * In: 4th Symposium on Advances in Spatial Databases, SSD'95
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 * @apiviz.uses DoubleDistanceSearchCandidate
 */
@Reference(authors = "G. R. Hjaltason, H. Samet", //
title = "Ranking in spatial databases", //
booktitle = "Advances in Spatial Databases - 4th Symposium, SSD'95", //
url = "http://dx.doi.org/10.1007/3-540-60159-7_6")
public class RStarTreeKNNQuery<O extends SpatialComparable> implements KNNQuery<O> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?, ?> tree;

  /**
   * Spatial primitive distance function.
   */
  protected final SpatialPrimitiveDistanceFunction<? super O> distanceFunction;

  /**
   * Relation we query.
   */
  protected Relation<? extends O> relation;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Data relation to query
   * @param distanceFunction Distance function
   */
  public RStarTreeKNNQuery(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation, SpatialPrimitiveDistanceFunction<? super O> distanceFunction) {
    super();
    this.relation = relation;
    this.tree = tree;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one neighbor has to be requested!");
    }
    tree.statistics.countKNNQuery();

    final KNNHeap knnList = DBIDUtil.newHeap(k);
    final ComparableMinHeap<DoubleDistanceSearchCandidate> pq = new ComparableMinHeap<>(Math.min(knnList.getK() << 1, 21));

    // expand root
    double maxDist = expandNode(obj, knnList, pq, Double.MAX_VALUE, tree.getRootID());

    // search in tree
    while(!pq.isEmpty()) {
      DoubleDistanceSearchCandidate pqNode = pq.poll();

      if(pqNode.mindist > maxDist) {
        break;
      }
      maxDist = expandNode(obj, knnList, pq, maxDist, pqNode.nodeID);
    }
    return knnList.toKNNList();
  }

  private double expandNode(O object, KNNHeap knnList, final ComparableMinHeap<DoubleDistanceSearchCandidate> pq, double maxDist, final int nodeID) {
    AbstractRStarTreeNode<?, ?> node = tree.getNode(nodeID);
    // data node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialPointLeafEntry entry = (SpatialPointLeafEntry) node.getEntry(i);
        double distance = distanceFunction.minDist(entry, object);
        tree.statistics.countDistanceCalculation();
        if(distance <= maxDist) {
          maxDist = knnList.insert(distance, entry.getDBID());
        }
      }
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
        double distance = distanceFunction.minDist(entry, object);
        tree.statistics.countDistanceCalculation();
        // Greedy expand, bypassing the queue
        if(distance <= 0) {
          expandNode(object, knnList, pq, maxDist, entry.getPageID());
        }
        else {
          if(distance <= maxDist) {
            pq.add(new DoubleDistanceSearchCandidate(distance, entry.getPageID()));
          }
        }
      }
    }
    return maxDist;
  }

  /**
   * Performs a batch knn query.
   * 
   * @param node the node for which the query should be performed
   * @param knnLists a map containing the knn lists for each query objects
   */
  protected void batchNN(AbstractRStarTreeNode<?, ?> node, Map<DBID, KNNHeap> knnLists) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry p = node.getEntry(i);
        for(Entry<DBID, KNNHeap> ent : knnLists.entrySet()) {
          final DBID q = ent.getKey();
          final KNNHeap knns_q = ent.getValue();
          double knn_q_maxDist = knns_q.getKNNDistance();

          DBID pid = ((LeafEntry) p).getDBID();
          // FIXME: objects are NOT accessible by DBID in a plain R-tree
          // context!
          double dist_pq = distanceFunction.distance(relation.get(pid), relation.get(q));
          tree.statistics.countDistanceCalculation();
          if(dist_pq <= knn_q_maxDist) {
            knns_q.insert(dist_pq, pid);
          }
        }
      }
    }
    else {
      ModifiableDBIDs ids = DBIDUtil.newArray(knnLists.size());
      for(DBID id : knnLists.keySet()) {
        ids.add(id);
      }
      List<DoubleDistanceEntry> entries = getSortedEntries(node, ids);
      for(DoubleDistanceEntry distEntry : entries) {
        double minDist = distEntry.distance;
        for(Entry<DBID, KNNHeap> ent : knnLists.entrySet()) {
          final KNNHeap knns_q = ent.getValue();
          double knn_q_maxDist = knns_q.getKNNDistance();

          if(minDist <= knn_q_maxDist) {
            SpatialEntry entry = distEntry.entry;
            AbstractRStarTreeNode<?, ?> child = tree.getNode(((DirectoryEntry) entry).getPageID().intValue());
            batchNN(child, knnLists);
            break;
          }
        }
      }
    }
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   * 
   * @param node the node
   * @param ids the id of the objects
   * @return a list of the sorted entries
   */
  protected List<DoubleDistanceEntry> getSortedEntries(AbstractRStarTreeNode<?, ?> node, DBIDs ids) {
    List<DoubleDistanceEntry> result = new ArrayList<>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      SpatialEntry entry = node.getEntry(i);
      double minMinDist = Double.MAX_VALUE;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double minDist = distanceFunction.minDist(entry, relation.get(iter));
        tree.statistics.countDistanceCalculation();
        minMinDist = Math.min(minDist, minMinDist);
      }
      result.add(new DoubleDistanceEntry(entry, minMinDist));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Optimized double distance entry implementation.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.hidden
   */
  static class DoubleDistanceEntry implements Comparable<DoubleDistanceEntry> {
    /**
     * Referenced entry
     */
    SpatialEntry entry;

    /**
     * Distance value
     */
    double distance;

    /**
     * Constructor.
     * 
     * @param entry Entry
     * @param distance Distance
     */
    public DoubleDistanceEntry(SpatialEntry entry, double distance) {
      this.entry = entry;
      this.distance = distance;
    }

    @Override
    public int compareTo(DoubleDistanceEntry o) {
      return Double.compare(this.distance, o.distance);
    }
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    // While this works, it seems to be slow at least for large sets!
    // TODO: use a DataStore instead of a map.
    final Map<DBID, KNNHeap> knnLists = new HashMap<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      knnLists.put(id, DBIDUtil.newHeap(k));
    }

    batchNN(tree.getRoot(), knnLists);

    List<KNNList> result = new ArrayList<>();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      tree.statistics.countKNNQuery();
      result.add(knnLists.get(id).toKNNList());
    }
    return result;
  }
}