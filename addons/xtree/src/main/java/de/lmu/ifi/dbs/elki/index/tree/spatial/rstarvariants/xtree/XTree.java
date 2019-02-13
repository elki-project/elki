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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The XTree is a spatial index structure extending the R*-Tree.
 * <p>
 * Reference:
 * <p>
 * Stefan Berchtold, Daniel A. Keim, Hans-Peter Kriegel: The X-tree: An Index
 * Structure for High-Dimensional Data<br>
 * In Proc. 22nd Int. Conf. on Very Large Data Bases (VLDB'96).
 * 
 * @author Marisa Thoma
 * @since 0.7.5
 */
@Title("X-Tree")
@Description("Index structure for High-Dimensional data")
@Reference(authors = "Stefan Berchtold, Daniel A. Keim, Hans-Peter Kriegel", //
    title = "The X-tree: An Index Structure for High-Dimensional Data", //
    booktitle = "Proc. 22nd Int. Conf. on Very Large Data Bases (VLDB'96)", //
    url = "http://www.vldb.org/conf/1996/P028.PDF", //
    bibkey = "DBLP:conf/vldb/BerchtoldKK96")
public class XTree extends AbstractXTree<XTreeNode> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XTree.class);

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param settings Settings
   */
  public XTree(PageFile<XTreeNode> pagefile, XTreeSettings settings) {
    super(pagefile, settings);
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new XTreeDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(XTreeNode node) {
    return new XTreeDirectoryEntry(node.getPageID(), node.computeMBR());
  }

  @Override
  protected XTreeNode createNewLeafNode() {
    return new XTreeNode(leafCapacity, true);
  }

  @Override
  protected XTreeNode createNewDirectoryNode() {
    return new XTreeNode(dirCapacity, false);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "X-Tree";
  }

  @Override
  public String getShortName() {
    return "x-tree";
  }

  @Override
  protected Class<XTreeNode> getNodeClass() {
    return XTreeNode.class;
  }
}
