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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnified;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k &lt;= k_max. The k-nearest neighbor distance for k = k_max is stored
 * in each entry of a node.
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @navhas - contains - MkMaxTreeNode
 *
 * @param <O> the type of DatabaseObject to be stored in the MkMaxTree
 */
public abstract class MkMaxTree<O> extends AbstractMkTreeUnified<O, MkMaxTreeNode<O>, MkMaxEntry, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MkMaxTree.class);

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MkMaxTree(Relation<O> relation, PageFile<MkMaxTreeNode<O>> pagefile, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry> settings) {
    super(relation, pagefile, settings);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. In the
   * first step the candidates are chosen by performing a reverse k-nearest
   * neighbor query with k = {@link #getKmax()}. Then these candidates are refined
   * in a second step.
   */
  @Override
  public DoubleDBIDList reverseKNNQuery(DBIDRef id, int k) {
    if (k > this.getKmax()) {
      throw new IllegalArgumentException("Parameter k has to be equal or less than " + "parameter k of the MkMax-Tree!");
    }

    // get the candidates
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList();
    doReverseKNNQuery(id, getRoot(), null, candidates);

    if (k == this.getKmax()) {
      candidates.sort();
      // FIXME: re-add statistics.
      // rkNNStatistics.addTrueHits(candidates.size());
      // rkNNStatistics.addResults(candidates.size());
      return candidates;
    }

    // refinement of candidates
    ModifiableDBIDs candidateIDs = DBIDUtil.newArray(candidates.size());
    for (DBIDIter candidate = candidates.iter(); candidate.valid(); candidate.advance()) {
      candidateIDs.add(candidate);
    }
    Map<DBID, KNNList> knnLists = batchNN(getRoot(), candidateIDs, k);

    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for (DBIDIter iter = candidateIDs.iter(); iter.valid(); iter.advance()) {
      DBID cid = DBIDUtil.deref(iter);
      KNNList cands = knnLists.get(cid);
      for (DoubleDBIDListIter iter2 = cands.iter(); iter2.valid(); iter2.advance()) {
        if (DBIDUtil.equal(id, iter2)) {
          result.add(iter2.doubleValue(), cid);
          break;
        }
      }
    }

    // FIXME: re-add statistics.
    // rkNNStatistics.addResults(result.size());
    // rkNNStatistics.addCandidates(candidates.size());
    result.sort();
    return result;
  }

  /**
   * Adapts the knn distances before insertion of the specified entry.
   *
   */
  @Override
  protected void preInsert(MkMaxEntry entry) {
    KNNHeap knns_o = DBIDUtil.newHeap(getKmax());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   */
  @Override
  protected void kNNdistanceAdjustment(MkMaxEntry entry, Map<DBID, KNNList> knnLists) {
    MkMaxTreeNode<O> node = getNode(entry);
    double knnDist_node = 0.;
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry leafEntry = node.getEntry(i);
        leafEntry.setKnnDistance(knnLists.get(leafEntry.getRoutingObjectID()).getKNNDistance());
        knnDist_node = Math.max(knnDist_node, leafEntry.getKnnDistance());
      }
    } else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry dirEntry = node.getEntry(i);
        kNNdistanceAdjustment(dirEntry, knnLists);
        knnDist_node = Math.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    entry.setKnnDistance(knnDist_node);
  }

  /**
   * Performs a reverse k-nearest neighbor query in the specified subtree for
   * the given query object with k = {@link #getKmax()}. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   *
   * @param q the id of the query object
   * @param node the node of the subtree on which the query is performed
   * @param node_entry the entry representing the node
   * @param result the list for the query result
   */
  private void doReverseKNNQuery(DBIDRef q, MkMaxTreeNode<O> node, MkMaxEntry node_entry, ModifiableDoubleDBIDList result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry entry = node.getEntry(i);
        double distance = distance(entry.getRoutingObjectID(), q);
        if (distance <= entry.getKnnDistance()) {
          result.add(distance, entry.getRoutingObjectID());
        }
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry entry = node.getEntry(i);
        double node_knnDist = node_entry != null ? node_entry.getKnnDistance() : Double.POSITIVE_INFINITY;

        double distance = distance(entry.getRoutingObjectID(), q);
        double minDist = (entry.getCoveringRadius() > distance) ? 0.0 : distance - entry.getCoveringRadius();

        if (minDist <= node_knnDist) {
          MkMaxTreeNode<O> childNode = getNode(entry);
          doReverseKNNQuery(q, childNode, entry, result);
        }
      }
    }
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   *
   * @param q the entry to be inserted
   * @param nodeEntry the entry representing the root of the current subtree
   * @param knns_q the knns of q
   */
  private void preInsert(MkMaxEntry q, MkMaxEntry nodeEntry, KNNHeap knns_q) {
    if (LOG.isDebugging()) {
      LOG.debugFine("preInsert " + q + " - " + nodeEntry + "\n");
    }

    double knnDist_q = knns_q.getKNNDistance();
    MkMaxTreeNode<O> node = getNode(nodeEntry);
    double knnDist_node = 0.;

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry p = node.getEntry(i);
        double dist_pq = distance(p.getRoutingObjectID(), q.getRoutingObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq <= knnDist_q) {
          knns_q.insert(dist_pq, p.getRoutingObjectID());
          if (knns_q.size() >= getKmax()) {
            knnDist_q = knns_q.getKNNDistance();
            q.setKnnDistance(knnDist_q);
          }
        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist_pq <= p.getKnnDistance()) {
          KNNList knns_p = knnq.getKNNForDBID(p.getRoutingObjectID(), getKmax() - 1);

          if (knns_p.size() + 1 < getKmax()) {
            p.setKnnDistance(Double.NaN);
          } else {
            double knnDist_p = Math.max(dist_pq, knns_p.getKNNDistance());
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = Math.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DoubleIntPair> entries = getSortedEntries(node, q.getRoutingObjectID());
      for (DoubleIntPair distEntry : entries) {
        MkMaxEntry dirEntry = node.getEntry(distEntry.second);
        double entry_knnDist = dirEntry.getKnnDistance();

        if (distEntry.second < entry_knnDist || distEntry.second < knnDist_q) {
          preInsert(q, dirEntry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = Math.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    if (LOG.isDebugging()) {
      LOG.debugFine(nodeEntry + "set knn dist " + knnDist_node);
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  @Override
  protected void initializeCapacities(MkMaxEntry exampleLeaf) {
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE; // exampleLeaf.getParentDistance().externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (file.getPageSize() - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + knnDistance) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + 4 + 3 * distanceSize) + 1;

    if (dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if (dirCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (file.getPageSize() - overhead) / (objectID +
    // parentDistance +
    // knnDistance) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + 2 * distanceSize) + 1;

    if (leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if (leafCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }
  }

  /**
   * @return a new MkMaxTreeNode which is a leaf node
   */
  @Override
  protected MkMaxTreeNode<O> createNewLeafNode() {
    return new MkMaxTreeNode<>(leafCapacity, true);
  }

  /**
   * @return a new MkMaxTreeNode which is a directory node
   */
  @Override
  protected MkMaxTreeNode<O> createNewDirectoryNode() {
    return new MkMaxTreeNode<>(dirCapacity, false);
  }

  /**
   * @return a new MkMaxDirectoryEntry representing the specified node
   */
  @Override
  protected MkMaxEntry createNewDirectoryEntry(MkMaxTreeNode<O> node, DBID routingObjectID, double parentDistance) {
    return new MkMaxDirectoryEntry(routingObjectID, parentDistance, node.getPageID(), node.coveringRadiusFromEntries(routingObjectID, this), node.kNNDistance());
  }

  /**
   * @return a new MkMaxDirectoryEntry by calling
   *         <code>new MkMaxDirectoryEntry(null, null, 0, null)</code>
   */
  @Override
  protected MkMaxEntry createRootEntry() {
    return new MkMaxDirectoryEntry(null, 0., 0, 0., 0.);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
