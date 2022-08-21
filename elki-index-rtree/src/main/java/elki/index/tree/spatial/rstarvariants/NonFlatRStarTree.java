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
package elki.index.tree.spatial.rstarvariants;

import java.util.ArrayList;
import java.util.List;

import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialEntry;
import elki.persistent.PageFile;

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

  @Override
  protected boolean hasOverflow(N node) {
    return node.getNumEntries() == (node.isLeaf() ? leafCapacity : dirCapacity);
  }

  @Override
  protected boolean hasUnderflow(N node) {
    return node.getNumEntries() < (node.isLeaf() ? leafMinimum : dirMinimum);
  }

  @Override
  protected int computeHeight() {
    N node = getNode(getRootID());
    int height = 1;
    while(!node.isLeaf() && node.getNumEntries() != 0) {
      node = getNode(node.getEntry(0));
      height++;
    }
    return height;
  }

  @Override
  protected void createEmptyRoot(E exampleLeaf) {
    writeNode(createNewLeafNode());
    setHeight(1);
  }

  @Override
  protected void bulkLoad(List<E> spatialObjects) {
    if(!initialized) {
      initialize(spatialObjects.get(0));
    }

    StringBuilder msg = getLogger().isDebuggingFine() ? new StringBuilder(500) : null;

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
      getLogger().debugFine(msg.append("\n  height = ").append(getHeight()) //
          .append("\n  root ").append(getNode(getRootID())).toString());
    }
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   * 
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private List<E> createBulkDirectoryNodes(List<E> nodes) {
    int minEntries = dirMinimum, maxEntries = dirCapacity - 1;

    ArrayList<E> result = new ArrayList<>();
    for(List<E> partition : settings.bulkSplitter.partition(nodes, minEntries, maxEntries)) {
      N dirNode = createNewDirectoryNode();
      for(E o : partition) {
        dirNode.addEntry(o);
      }
      writeNode(dirNode);

      result.add(createNewDirectoryEntry(dirNode));
      if(getLogger().isDebuggingFiner()) {
        getLogger().debugFiner("Directory page no: " + dirNode.getPageID());
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
    for(E entry : objects) {
      root.addEntry(entry);
    }
    ((SpatialDirectoryEntry) getRootEntry()).setMBR(root.computeMBR());

    writeNode(root);
    if(getLogger().isDebuggingFiner()) {
      getLogger().debugFiner("pageNo " + root.getPageID());
    }
    return root;
  }
}
