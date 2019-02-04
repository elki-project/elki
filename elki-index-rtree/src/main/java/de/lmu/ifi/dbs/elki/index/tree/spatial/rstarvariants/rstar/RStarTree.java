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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * RStarTree is a spatial index structure based on the concepts of the R*-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure and ensures persistence.
 * <p>
 * Reference:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @navhas - contains - RStarTreeNode
 */
@Title("R*-Tree")
@Description("Balanced index structure based on bounding rectangles.")
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
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