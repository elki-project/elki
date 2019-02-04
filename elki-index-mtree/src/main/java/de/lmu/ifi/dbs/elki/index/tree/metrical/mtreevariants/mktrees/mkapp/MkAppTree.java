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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeSearchCandidate;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.PolynomialRegression;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import net.jafama.FastMath;

/**
 * MkAppTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k &lt; kmax.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - MkAppTreeSettings
 * @navhas - contains - MkAppTreeNode
 *
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 */
public abstract class MkAppTree<O> extends AbstractMkTree<O, MkAppTreeNode<O>, MkAppEntry, MkAppTreeSettings<O>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MkAppTree.class);

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param pageFile Page file
   * @param settings Tree settings
   */
  public MkAppTree(Relation<O> relation, PageFile<MkAppTreeNode<O>> pageFile, MkAppTreeSettings<O> settings) {
    super(relation, pageFile, settings);
  }

  /**
   * @throws UnsupportedOperationException since this operation is not supported
   */
  @Override
  public void insert(MkAppEntry id, boolean withPreInsert) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * @throws UnsupportedOperationException since this operation is not supported
   */
  @Override
  protected void preInsert(MkAppEntry entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Inserts the specified objects into this MkApp-Tree.
   *
   * @param entries the entries to be inserted
   */
  @Override
  public void insertAll(List<MkAppEntry> entries) {
    if(entries.isEmpty()) {
      return;
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("insert " + entries + "\n");
    }

    if(!initialized) {
      initialize(entries.get(0));
    }

    ModifiableDBIDs ids = DBIDUtil.newArray(entries.size());

    // insert
    for(MkAppEntry entry : entries) {
      ids.add(entry.getRoutingObjectID());
      // insert the object
      super.insert(entry, false);
    }

    // do batch nn
    Map<DBID, KNNList> knnLists = batchNN(getRoot(), ids, settings.kmax + 1);

    // adjust the knn distances
    adjustApproximatedKNNDistances(getRootEntry(), knnLists);

    if(EXTRA_INTEGRITY_CHECKS) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param id the query object id
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  @Override
  public DoubleDBIDList reverseKNNQuery(DBIDRef id, int k) {
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    final Heap<MTreeSearchCandidate> pq = new UpdatableHeap<>();

    // push root
    pq.add(new MTreeSearchCandidate(0., getRootID(), null, Double.NaN));

    // search in tree
    while(!pq.isEmpty()) {
      MTreeSearchCandidate pqNode = pq.poll();
      // FIXME: cache the distance to the routing object in the queue node!

      MkAppTreeNode<O> node = getNode(pqNode.nodeID);

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkAppEntry entry = node.getEntry(i);
          double distance = distance(entry.getRoutingObjectID(), id);
          double minDist = (entry.getCoveringRadius() > distance) ? 0. : distance - entry.getCoveringRadius();

          double approxValue = settings.log ? FastMath.exp(entry.approximatedValueAt(k)) : entry.approximatedValueAt(k);
          if(approxValue < 0) {
            approxValue = 0;
          }

          if(minDist <= approxValue) {
            pq.add(new MTreeSearchCandidate(minDist, getPageID(entry), entry.getRoutingObjectID(), Double.NaN));
          }
        }
      }
      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MkAppLeafEntry entry = (MkAppLeafEntry) node.getEntry(i);
          double distance = distance(entry.getRoutingObjectID(), id);
          double approxValue = settings.log ? FastMath.exp(entry.approximatedValueAt(k)) : entry.approximatedValueAt(k);
          if(approxValue < 0) {
            approxValue = 0;
          }

          if(distance <= approxValue) {
            result.add(distance, entry.getRoutingObjectID());
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns the value of the k_max parameter.
   *
   * @return the value of the k_max parameter
   */
  public int getK_max() {
    return settings.kmax;
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   */
  @Override
  protected void initializeCapacities(MkAppEntry exampleLeaf) {
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE; // exampleLeaf.getParentDistance().externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (file.getPageSize() - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + approx) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + 4 + distanceSize + distanceSize + (settings.p + 1) * 4 + 2) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (file.getPageSize() - overhead) / (objectID +
    // parentDistance +
    // approx) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + distanceSize + (settings.p + 1) * 4 + 2) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    initialized = true;

    if(LOG.isVerbose()) {
      LOG.verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
    }
  }

  private double[] getMeanKNNList(DBIDs ids, Map<DBID, KNNList> knnLists) {
    double[] means = new double[settings.kmax];
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      KNNList knns = knnLists.get(id);
      int k = 0;
      for(DoubleDBIDListIter it = knns.iter(); k < settings.kmax && it.valid(); it.advance(), k++) {
        means[k] += it.doubleValue();
      }
    }

    for(int k = 0; k < settings.kmax; k++) {
      means[k] /= ids.size();
    }

    return means;
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustApproximatedKNNDistances(MkAppEntry entry, Map<DBID, KNNList> knnLists) {
    MkAppTreeNode<O> node = getNode(entry);

    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkAppLeafEntry leafEntry = (MkAppLeafEntry) node.getEntry(i);
        // approximateKnnDistances(leafEntry,
        // getKNNList(leafEntry.getRoutingObjectID(), knnLists));
        PolynomialApproximation approx = approximateKnnDistances(getMeanKNNList(leafEntry.getDBID(), knnLists));
        leafEntry.setKnnDistanceApproximation(approx);
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkAppEntry dirEntry = node.getEntry(i);
        adjustApproximatedKNNDistances(dirEntry, knnLists);
      }
    }

    // PolynomialApproximation approx1 = node.knnDistanceApproximation();
    ArrayModifiableDBIDs ids = DBIDUtil.newArray();
    leafEntryIDs(node, ids);
    PolynomialApproximation approx = approximateKnnDistances(getMeanKNNList(ids, knnLists));
    entry.setKnnDistanceApproximation(approx);
  }

  /**
   * Determines the ids of the leaf entries stored in the specified subtree.
   *
   * @param node the root of the subtree
   * @param result the result list containing the ids of the leaf entries stored
   *        in the specified subtree
   */
  private void leafEntryIDs(MkAppTreeNode<O> node, ModifiableDBIDs result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkAppEntry entry = node.getEntry(i);
        result.add(((LeafEntry) entry).getDBID());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkAppTreeNode<O> childNode = getNode(node.getEntry(i));
        leafEntryIDs(childNode, result);
      }
    }
  }

  /**
   * Computes the polynomial approximation of the specified knn-distances.
   *
   * @param knnDistances the knn-distances of the leaf entry
   * @return the polynomial approximation of the specified knn-distances.
   */
  private PolynomialApproximation approximateKnnDistances(double[] knnDistances) {
    StringBuilder msg = new StringBuilder();

    // count the zero distances (necessary of log-log space is used)
    int k_0 = 0;
    if(settings.log) {
      for(int i = 0; i < settings.kmax; i++) {
        double dist = knnDistances[i];
        if(dist == 0) {
          k_0++;
        }
        else {
          break;
        }
      }
    }

    double[] x = new double[settings.kmax - k_0];
    double[] y = new double[settings.kmax - k_0];

    for(int k = 0; k < settings.kmax - k_0; k++) {
      if(settings.log) {
        x[k] = FastMath.log(k + k_0);
        y[k] = FastMath.log(knnDistances[k + k_0]);
      }
      else {
        x[k] = k + k_0;
        y[k] = knnDistances[k + k_0];
      }
    }

    PolynomialRegression regression = new PolynomialRegression(y, x, settings.p);
    PolynomialApproximation approximation = new PolynomialApproximation(regression.getEstimatedCoefficients());

    if(LOG.isDebugging()) {
      msg.append("approximation ").append(approximation);
      LOG.debugFine(msg.toString());
    }
    return approximation;

  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  @Override
  protected MkAppTreeNode<O> createNewLeafNode() {
    return new MkAppTreeNode<>(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected MkAppTreeNode<O> createNewDirectoryNode() {
    return new MkAppTreeNode<>(dirCapacity, false);
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
  protected MkAppEntry createNewDirectoryEntry(MkAppTreeNode<O> node, DBID routingObjectID, double parentDistance) {
    return new MkAppDirectoryEntry(routingObjectID, parentDistance, node.getPageID(), node.coveringRadiusFromEntries(routingObjectID, this), null);
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  @Override
  protected MkAppEntry createRootEntry() {
    return new MkAppDirectoryEntry(null, 0., 0, 0., null);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
