package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.dbs.elki.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.SplitStrategy;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * DeLiCluTree is a spatial index structure based on an R-TRee. DeLiCluTree is
 * designed for the DeLiClu algorithm, having in each node a boolean array which
 * indicates whether the child nodes are already handled by the DeLiClu
 * algorithm.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has DeLiCluNode oneway - - contains
 */
public class DeLiCluTree extends NonFlatRStarTree<DeLiCluNode, DeLiCluEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DeLiCluTree.class);

  /**
   * Holds the ids of the expanded nodes.
   */
  private HashMap<Integer, HashSet<Integer>> expanded = new HashMap<Integer, HashSet<Integer>>();

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param bulkSplitter bulk load strategy
   * @param insertionStrategy the strategy to find the insertion child
   * @param nodeSplitter the strategy for splitting nodes.
   */
  public DeLiCluTree(PageFile<DeLiCluNode> pagefile, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, SplitStrategy nodeSplitter) {
    super(pagefile, bulkSplitter, insertionStrategy, nodeSplitter);
  }

  /**
   * Marks the nodes with the specified ids as expanded.
   * 
   * @param entry1 the first node
   * @param entry2 the second node
   */
  public void setExpanded(SpatialEntry entry1, SpatialEntry entry2) {
    HashSet<Integer> exp1 = expanded.get(getPageID(entry1));
    if(exp1 == null) {
      exp1 = new HashSet<Integer>();
      expanded.put(getPageID(entry1), exp1);
    }
    exp1.add(getPageID(entry2));
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   * 
   * @param entry the id of the node for which the expansions should be returned
   * @return the nodes which are already expanded with the specified node
   */
  public Set<Integer> getExpanded(SpatialEntry entry) {
    HashSet<Integer> exp = expanded.get(getPageID(entry));
    if(exp != null) {
      return exp;
    }
    return new HashSet<Integer>();
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   * 
   * @param entry the id of the node for which the expansions should be returned
   * @return the nodes which are already expanded with the specified node
   */
  public Set<Integer> getExpanded(DeLiCluNode entry) {
    HashSet<Integer> exp = expanded.get(entry.getPageID());
    if(exp != null) {
      return exp;
    }
    return new HashSet<Integer>();
  }

  /**
   * Determines and returns the number of nodes in this index.
   * 
   * @return the number of nodes in this index
   */
  public int numNodes() {
    int numNodes = 0;

    BreadthFirstEnumeration<DeLiCluNode, DeLiCluEntry> bfs = new BreadthFirstEnumeration<DeLiCluNode, DeLiCluEntry>(this, getRootPath());
    while(bfs.hasMoreElements()) {
      Entry entry = bfs.nextElement().getLastPathComponent().getEntry();
      if(!entry.isLeafEntry()) {
        numNodes++;
      }
    }

    return numNodes;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @return a new leaf node
   */
  @Override
  protected DeLiCluNode createNewLeafNode() {
    return new DeLiCluNode(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @return a new directory node
   */
  @Override
  protected DeLiCluNode createNewDirectoryNode() {
    return new DeLiCluNode(dirCapacity, false);
  }

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   */
  @Override
  protected DeLiCluEntry createNewDirectoryEntry(DeLiCluNode node) {
    return new DeLiCluDirectoryEntry(node.getPageID(), node.computeMBR(), node.hasHandled(), node.hasUnhandled());
  }

  /**
   * Creates an entry representing the root node.
   * 
   * @return an entry representing the root node
   */
  @Override
  protected DeLiCluEntry createRootEntry() {
    return new DeLiCluDirectoryEntry(0, null, false, true);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}