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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

/**
 * DeLiCluTree is a spatial index structure based on an R-Tree. DeLiCluTree is
 * designed for the DeLiClu algorithm, having in each node a boolean array which
 * indicates whether the child nodes are already handled by the DeLiClu
 * algorithm.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @navhas - contains - DeLiCluNode
 */
public abstract class DeLiCluTree extends NonFlatRStarTree<DeLiCluNode, DeLiCluEntry, RTreeSettings> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DeLiCluTree.class);

  /**
   * Holds the ids of the expanded nodes.
   */
  private Int2ObjectOpenHashMap<IntSet> expanded = new Int2ObjectOpenHashMap<>();

  /**
   * Constructor.
   *
   * @param pagefile Page file
   * @param settings Settings
   */
  public DeLiCluTree(PageFile<DeLiCluNode> pagefile, RTreeSettings settings) {
    super(pagefile, settings);
  }

  /**
   * Marks the nodes with the specified ids as expanded.
   *
   * @param entry1 the first node
   * @param entry2 the second node
   */
  public void setExpanded(SpatialEntry entry1, SpatialEntry entry2) {
    IntSet exp1 = expanded.get(getPageID(entry1));
    if(exp1 == null) {
      exp1 = new IntOpenHashSet();
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
  public IntSet getExpanded(SpatialEntry entry) {
    IntSet exp = expanded.get(getPageID(entry));
    return (exp != null) ? exp : IntSets.EMPTY_SET;
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   *
   * @param entry the id of the node for which the expansions should be returned
   * @return the nodes which are already expanded with the specified node
   */
  public IntSet getExpanded(DeLiCluNode entry) {
    IntSet exp = expanded.get(entry.getPageID());
    return (exp != null) ? exp : IntSets.EMPTY_SET;
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
    return LOG;
  }
}
