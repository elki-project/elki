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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract superclass for all non-flat R*-Tree variants.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class NonFlatRStarTree<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry, S extends RTreeSettings> extends AbstractRStarTree<N, E, S> {
  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param settings Settings
   */
  public NonFlatRStarTree(PageFile<N> pagefile, S settings) {
    super(pagefile, settings);
  }

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  @Override
  protected boolean hasOverflow(N node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }
    else {
      return node.getNumEntries() == dirCapacity;
    }
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
  protected boolean hasUnderflow(N node) {
    if(node.isLeaf()) {
      return node.getNumEntries() < leafMinimum;
    }
    else {
      return node.getNumEntries() < dirMinimum;
    }
  }

  /**
   * Computes the height of this RTree. Is called by the constructor. and should
   * be overwritten by subclasses if necessary.
   * 
   * @return the height of this RTree
   */
  @Override
  protected int computeHeight() {
    N node = getRoot();
    int height = 1;

    // compute height
    while(!node.isLeaf() && node.getNumEntries() != 0) {
      E entry = node.getEntry(0);
      node = getNode(entry);
      height++;
    }
    return height;
  }

  @Override
  protected void createEmptyRoot(E exampleLeaf) {
    N root = createNewLeafNode();
    writeNode(root);
    setHeight(1);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(List<E> spatialObjects) {
    if(!initialized) {
      initialize(spatialObjects.get(0));
    }

    StringBuilder msg = getLogger().isDebuggingFine() ? new StringBuilder() : null;

    // Tiny tree that fits into a single page
    if(spatialObjects.size() <= leafCapacity) {
      N root = createNewLeafNode();
      root.setPageID(getRootID());
      writeNode(root);
      createRoot(root, spatialObjects);
      setHeight(1);
      if(msg != null) {
        msg.append("\n  numNodes = 1");
      }
    }
    // root is directory node
    else {
      N root = createNewDirectoryNode();
      root.setPageID(getRootID());
      writeNode(root);

      // create leaf nodes
      List<E> nodes = createBulkLeafNodes(spatialObjects);

      int numNodes = nodes.size();
      if(msg != null) {
        msg.append("\n  numLeafNodes = ").append(numNodes);
      }
      setHeight(1);

      // create directory nodes
      while(nodes.size() > (dirCapacity - 1)) {
        nodes = createBulkDirectoryNodes(nodes);
        numNodes += nodes.size();
        setHeight(getHeight() + 1);
      }

      // create root
      createRoot(root, nodes);
      numNodes++;
      setHeight(getHeight() + 1);
      if(msg != null) {
        msg.append("\n  numNodes = ").append(numNodes);
      }
    }
    if(msg != null) {
      msg.append("\n  height = ").append(getHeight());
      msg.append("\n  root ").append(getRoot());
      getLogger().debugFine(msg.toString());
    }
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   * 
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private List<E> createBulkDirectoryNodes(List<E> nodes) {
    int minEntries = dirMinimum;
    int maxEntries = dirCapacity - 1;

    ArrayList<E> result = new ArrayList<>();
    List<List<E>> partitions = settings.bulkSplitter.partition(nodes, minEntries, maxEntries);

    for(List<E> partition : partitions) {
      // create node
      N dirNode = createNewDirectoryNode();
      // insert nodes
      for(E o : partition) {
        dirNode.addDirectoryEntry(o);
      }
      // write to file
      writeNode(dirNode);

      result.add(createNewDirectoryEntry(dirNode));
      if(getLogger().isDebuggingFiner()) {
        getLogger().debugFiner("Directory page no: "+dirNode.getPageID());
      }
    }

    return result;
  }

  /**
   * Returns a root node for bulk load. If the objects are data objects a leaf
   * node will be returned, if the objects are nodes a directory node will be
   * returned.
   * 
   * @param root the new root node
   * @param objects the spatial objects to be inserted
   * @return the root node
   */
  private N createRoot(N root, List<E> objects) {
    // insert data
    for(E entry : objects) {
      if (entry instanceof LeafEntry) {
        root.addLeafEntry(entry);
      }
      else {
        root.addDirectoryEntry(entry);
      }
    }

    // set root mbr
    ((SpatialDirectoryEntry) getRootEntry()).setMBR(root.computeMBR());

    // write to file
    writeNode(root);
    if(getLogger().isDebuggingFiner()) {
      StringBuilder msg = new StringBuilder();
      msg.append("pageNo ").append(root.getPageID());
      getLogger().debugFiner(msg.toString());
    }

    return root;
  }
}