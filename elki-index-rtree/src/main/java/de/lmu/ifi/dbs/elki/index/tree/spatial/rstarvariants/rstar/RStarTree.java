package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

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

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * RStarTree is a spatial index structure based on the concepts of the R*-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @apiviz.has RStarTreeNode oneway - - contains
 */
@Title("R*-Tree")
@Description("Balanced index structure based on bounding rectangles.")
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public abstract class RStarTree extends NonFlatRStarTree<RStarTreeNode, SpatialEntry, RTreeSettings> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RStarTree.class);

  /**
   * Constructor.
   *
   * @param pagefile Page file
   * @param settings Settings class
   */
  public RStarTree(PageFile<RStarTreeNode> pagefile, RTreeSettings settings) {
    super(pagefile, settings);
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(RStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getPageID(), node.computeMBR());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  @Override
  protected RStarTreeNode createNewLeafNode() {
    return new RStarTreeNode(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected RStarTreeNode createNewDirectoryNode() {
    return new RStarTreeNode(dirCapacity, false);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}