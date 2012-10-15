package experimentalcode.shared.index.xtree;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The XTree is a spatial index structure extending the R*-Tree.
 * 
 * <p>
 * Reference: <br>
 * Stefan Berchtold, Daniel A. Keim, Hans-Peter Kriegel: The X-tree: An Index
 * Structure for High-Dimensional Data<br>
 * In Proc. 22nd Int. Conf. on Very Large Data Bases (VLDB'96), Bombay, India,
 * 1996.
 * </p>
 * 
 * @author Marisa Thoma
 * @param <O> Database object type
 */
@Title("X-Tree")
@Description("Index structure for High-Dimensional data")
@Reference(authors = "S. Berchtold, D. A. Keim, H.-P. Kriegel", title = "The X-tree: An Index Structure for High-Dimensional Data", booktitle = "Proc. 22nd Int. Conf. on Very Large Data Bases (VLDB'96), Bombay, India, 1996", url = "http://www.vldb.org/conf/1996/P028.PDF")
public class XTree extends XTreeBase<XTreeNode, SpatialEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(XTree.class);

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param relativeMinEntries
   * @param relativeMinFanout
   * @param max_overlap
   * @param overlap_type
   */
  public XTree(PageFile<XTreeNode> pagefile, double relativeMinEntries, double relativeMinFanout, float max_overlap, XTreeBase.Overlap overlap_type) {
    super(pagefile, relativeMinEntries, relativeMinFanout, max_overlap, overlap_type);
  }

  /**
   * Creates an entry representing the root node.
   */
  @Override
  protected SpatialEntry createRootEntry() {
    return new XDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(XTreeNode node) {
    return new XDirectoryEntry(node.getPageID(), node.computeMBR());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @return a new leaf node
   */
  @Override
  protected XTreeNode createNewLeafNode() {
    return new XTreeNode(leafCapacity, true, SpatialPointLeafEntry.class);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @return a new directory node
   */
  @Override
  protected XTreeNode createNewDirectoryNode() {
    return new XTreeNode(dirCapacity, false, XDirectoryEntry.class);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  protected Class<XTreeNode> getNodeClass() {
    return XTreeNode.class;
  }
}