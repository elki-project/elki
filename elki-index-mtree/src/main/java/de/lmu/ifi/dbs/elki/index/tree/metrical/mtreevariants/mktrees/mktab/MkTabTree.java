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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnified;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;

/**
 * MkTabTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k &lt; kmax. All knn distances for k &lt;= kmax are stored in each entry
 * of a node.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @navhas - contains - MkTabTreeNode
 *
 * @param <O> Object type
 */
public abstract class MkTabTree<O> extends AbstractMkTreeUnified<O, MkTabTreeNode<O>, MkTabEntry, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MkTabTree.class);

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param pagefile Page file
   * @param settings Settings
   */
  public MkTabTree(Relation<O> relation, PageFile<MkTabTreeNode<O>> pagefile, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry> settings) {
    super(relation, pagefile, settings);
  }

  /**
   * @throws UnsupportedOperationException since insertion of single objects is
   *         not supported
   */
  @Override
  protected void preInsert(MkTabEntry entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * @throws UnsupportedOperationException since insertion of single objects is
   *         not supported
   */
  @Override
  public void insert(MkTabEntry entry, boolean withPreInsert) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  @Override
  public DoubleDBIDList reverseKNNQuery(DBIDRef id, int k) {
    if(k > this.getKmax()) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " + "parameter kmax of the MkTab-Tree!");
    }

    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    doReverseKNNQuery(k, id, null, getRoot(), result);

    result.sort();
    return result;
  }

  @Override
  protected void initializeCapacities(MkTabEntry exampleLeaf) {
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE; // exampleLeaf.getParentDistance().externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + kmax + kmax * knnDistance) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + 4 + distanceSize + distanceSize + 4 + getKmax() * distanceSize) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + +
    // kmax + kmax * knnDistance) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + distanceSize + 4 + getKmax() * distanceSize) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }
  }

  @Override
  protected void kNNdistanceAdjustment(MkTabEntry entry, Map<DBID, KNNList> knnLists) {
    MkTabTreeNode<O> node = getNode(entry);
    double[] knnDistances_node = initKnnDistanceList();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkTabLeafEntry leafEntry = (MkTabLeafEntry) node.getEntry(i);
        KNNList knns = knnLists.get(leafEntry.getDBID());
        double[] distances = new double[knns.size()];
        int j = 0;
        for(DoubleDBIDListIter iter = knns.iter(); iter.valid(); iter.advance(), j++) {
          distances[j] = iter.doubleValue();
        }
        leafEntry.setKnnDistances(distances);
        // FIXME: save copy
        knnDistances_node = max(knnDistances_node, leafEntry.getKnnDistances());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry dirEntry = node.getEntry(i);
        kNNdistanceAdjustment(dirEntry, knnLists);
        // FIXME: save copy
        knnDistances_node = max(knnDistances_node, dirEntry.getKnnDistances());
      }
    }
    entry.setKnnDistances(knnDistances_node);
  }

  @Override
  protected MkTabTreeNode<O> createNewLeafNode() {
    return new MkTabTreeNode<>(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected MkTabTreeNode<O> createNewDirectoryNode() {
    return new MkTabTreeNode<>(dirCapacity, false);
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance the distance from the routing object of the node to
   *        the routing object of the parent node
   */
  @Override
  protected MkTabEntry createNewDirectoryEntry(MkTabTreeNode<O> node, DBID routingObjectID, double parentDistance) {
    return new MkTabDirectoryEntry(routingObjectID, parentDistance, node.getPageID(), node.coveringRadiusFromEntries(routingObjectID, this), node.kNNDistances());
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  @Override
  protected MkTabEntry createRootEntry() {
    return new MkTabDirectoryEntry(null, 0., 0, 0., initKnnDistanceList());
  }

  /**
   * Performs a k-nearest neighbor query in the specified subtree for the given
   * query object and the given parameter k. It recursively traverses all paths
   * from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   *
   * @param k the parameter k of the knn-query
   * @param q the id of the query object
   * @param node_entry the entry representing the node
   * @param node the root of the subtree
   * @param result the list holding the query result
   */
  private void doReverseKNNQuery(int k, DBIDRef q, MkTabEntry node_entry, MkTabTreeNode<O> node, ModifiableDoubleDBIDList result) {
    // data node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry entry = node.getEntry(i);
        double distance = distance(entry.getRoutingObjectID(), q);
        if(distance <= entry.getKnnDistance(k)) {
          result.add(distance, entry.getRoutingObjectID());
        }
      }
    }

    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry entry = node.getEntry(i);
        double node_knnDist = node_entry != null ? node_entry.getKnnDistance(k) : Double.POSITIVE_INFINITY;

        double distance = distance(entry.getRoutingObjectID(), q);
        double minDist = (entry.getCoveringRadius() > distance) ? 0. : distance - entry.getCoveringRadius();

        if(minDist <= node_knnDist) {
          MkTabTreeNode<O> childNode = getNode(entry);
          doReverseKNNQuery(k, q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Returns an array that holds the maximum values of the both specified arrays
   * in each index.
   *
   * @param distances1 the first array
   * @param distances2 the second array
   * @return an array that holds the maximum values of the both specified arrays
   *         in each index
   */
  private double[] max(double[] distances1, double[] distances2) {
    if(distances1.length != distances2.length) {
      throw new RuntimeException("different lengths!");
    }

    double[] result = new double[distances1.length];
    for(int i = 0; i < distances1.length; i++) {
      result[i] = Math.max(distances1[i], distances2[i]);
    }
    return result;
  }

  /**
   * Returns a knn distance list with all distances set to null distance.
   *
   * @return a knn distance list with all distances set to null distance
   */
  private double[] initKnnDistanceList() {
    double[] knnDistances = new double[getKmax()];
    return knnDistances;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
