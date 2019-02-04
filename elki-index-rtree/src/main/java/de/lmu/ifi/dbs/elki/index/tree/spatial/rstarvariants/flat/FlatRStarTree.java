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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * FlatRTree is a spatial index structure based on a R*-Tree but with a flat
 * directory. Apart from organizing the objects it also provides several methods
 * to search for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert
 * @since 0.7.0
 *
 * @navhas - contains - FlatRStarTreeNode
 */
public abstract class FlatRStarTree extends AbstractRStarTree<FlatRStarTreeNode, SpatialEntry, RTreeSettings> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FlatRStarTree.class);

  /**
   * The root of this flat RTree.
   */
  private FlatRStarTreeNode root;

  /**
   * Constructor.
   *
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public FlatRStarTree(PageFile<FlatRStarTreeNode> pagefile, RTreeSettings settings) {
    super(pagefile, settings);
  }

  /**
   * Initializes the flat RTree from an existing persistent file.
   */
  @Override
  public void initializeFromFile(TreeIndexHeader header, PageFile<FlatRStarTreeNode> file) {
    super.initializeFromFile(header, file);

    // reconstruct root
    int nextPageID = file.getNextPageID();
    dirCapacity = nextPageID;
    root = createNewDirectoryNode();
    for(int i = 1; i < nextPageID; i++) {
      FlatRStarTreeNode node = getNode(i);
      root.addDirectoryEntry(createNewDirectoryEntry(node));
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("root: " + root + " with " + nextPageID + " leafNodes.");
    }
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  @Override
  public FlatRStarTreeNode getRoot() {
    return root;
  }

  /**
   * Returns the height of this FlatRTree.
   *
   * @return 2
   */
  @Override
  protected int computeHeight() {
    return 2;
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(List<SpatialEntry> spatialObjects) {
    if(!initialized) {
      initialize(spatialObjects.get(0));
    }
    // create leaf nodes
    getFile().setNextPageID(getRootID() + 1);
    List<SpatialEntry> nodes = createBulkLeafNodes(spatialObjects);
    int numNodes = nodes.size();
    if(LOG.isDebugging()) {
      LOG.debugFine("  numLeafNodes = " + numNodes);
    }

    // create root
    root = createNewDirectoryNode();
    root.setPageID(getRootID());
    for(SpatialEntry entry : nodes) {
      root.addDirectoryEntry(entry);
    }
    numNodes++;
    setHeight(2);

    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append("  root = ").append(getRoot());
      msg.append("\n  numNodes = ").append(numNodes);
      msg.append("\n  height = ").append(getHeight());
      LOG.debugFine(msg.toString() + "\n");
    }
    doExtraIntegrityChecks();
  }

  @Override
  protected void createEmptyRoot(SpatialEntry exampleLeaf) {
    root = createNewDirectoryNode();
    root.setPageID(getRootID());

    getFile().setNextPageID(getRootID() + 1);
    FlatRStarTreeNode leaf = createNewLeafNode();
    writeNode(leaf);
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(new double[exampleLeaf.getDimensionality()], new double[exampleLeaf.getDimensionality()]);
    root.addDirectoryEntry(new SpatialDirectoryEntry(leaf.getPageID(), mbr));

    setHeight(2);
  }

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  @Override
  protected boolean hasOverflow(FlatRStarTreeNode node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }
    else if(node.getNumEntries() == node.getCapacity()) {
      node.increaseEntries();
    }
    return false;
  }

  /**
   * Returns true if in the specified node an underflow occurred, false
   * otherwise.
   *
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false
   *         otherwise
   */
  @Override
  protected boolean hasUnderflow(FlatRStarTreeNode node) {
    if(node.isLeaf()) {
      return node.getNumEntries() < leafMinimum;
    }
    else {
      return false;
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  @Override
  protected FlatRStarTreeNode createNewLeafNode() {
    return new FlatRStarTreeNode(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected FlatRStarTreeNode createNewDirectoryNode() {
    return new FlatRStarTreeNode(dirCapacity, false);
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(FlatRStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getPageID(), node.computeMBR());
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}