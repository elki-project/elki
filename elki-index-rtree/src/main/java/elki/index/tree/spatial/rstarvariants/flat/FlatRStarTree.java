/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.spatial.rstarvariants.flat;

import java.util.List;

import elki.data.ModifiableHyperBoundingBox;
import elki.index.tree.TreeIndexHeader;
import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import elki.index.tree.spatial.rstarvariants.RTreeSettings;
import elki.logging.Logging;
import elki.persistent.PageFile;

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

  @Override
  public void initializeFromFile(TreeIndexHeader header, PageFile<FlatRStarTreeNode> file) {
    super.initializeFromFile(header, file);

    // reconstruct root
    int nextPageID = file.getNextPageID();
    dirCapacity = nextPageID;
    root = createNewDirectoryNode();
    for(int i = 1; i < nextPageID; i++) {
      root.addEntry(createNewDirectoryEntry(getNode(i)));
    }

    if(LOG.isDebugging()) {
      LOG.debugFine("root: " + root + " with " + nextPageID + " leafNodes.");
    }
  }

  @Override
  public FlatRStarTreeNode getNode(int nodeID) {
    return nodeID == getRootID() ? root : super.getNode(nodeID);
  }

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
    // create root
    root = createNewDirectoryNode();
    root.setPageID(getRootID());
    writeNode(root);
    // create leaf nodes
    List<SpatialEntry> nodes = createBulkLeafNodes(spatialObjects);
    int numNodes = nodes.size();
    if(LOG.isDebugging()) {
      LOG.debugFine("  numLeafNodes = " + numNodes);
    }

    root.increaseEntries(nodes.size());
    for(SpatialEntry entry : nodes) {
      root.addEntry(entry);
    }
    ((SpatialDirectoryEntry) getRootEntry()).setMBR(root.computeMBR());
    writeNode(root);
    numNodes++;
    setHeight(2);

    if(LOG.isDebuggingFine()) {
      LOG.debugFine("  root = " + root + "\n  numNodes = " + numNodes + "\n  height = " + getHeight());
    }
    doExtraIntegrityChecks();
  }

  @Override
  protected void createEmptyRoot(SpatialEntry exampleLeaf) {
    root = createNewDirectoryNode();
    root.setPageID(getRootID());
    writeNode(root);

    FlatRStarTreeNode leaf = createNewLeafNode();
    writeNode(leaf);
    assert leaf.getPageID() != root.getPageID() : "Page numbering inconsistent!?!";
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(new double[exampleLeaf.getDimensionality()], new double[exampleLeaf.getDimensionality()]);
    root.addEntry(new SpatialDirectoryEntry(leaf.getPageID(), mbr));
    writeNode(root);

    setHeight(2);
  }

  @Override
  protected boolean hasOverflow(FlatRStarTreeNode node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }
    if(node.getNumEntries() == node.getCapacity()) {
      node.increaseEntries(node.getCapacity() + 1);
    }
    return false;
  }

  @Override
  protected boolean hasUnderflow(FlatRStarTreeNode node) {
    return node.isLeaf() && node.getNumEntries() < leafMinimum;
  }

  @Override
  protected FlatRStarTreeNode createNewLeafNode() {
    return new FlatRStarTreeNode(leafCapacity, true);
  }

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
