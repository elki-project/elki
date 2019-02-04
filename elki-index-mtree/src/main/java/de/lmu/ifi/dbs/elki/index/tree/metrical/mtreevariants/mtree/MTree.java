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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br>
 * In Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MTreeNode
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 */
@Title("M-Tree")
@Description("Efficient Access Method for Similarity Search in Metric Spaces")
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
abstract public class MTree<O> extends AbstractMTree<O, MTreeNode<O>, MTreeEntry, MTreeSettings<O, MTreeNode<O>, MTreeEntry>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MTree.class);

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param settings Tree settings
   */
  public MTree(PageFile<MTreeNode<O>> pagefile, MTreeSettings<O, MTreeNode<O>, MTreeEntry> settings) {
    super(pagefile, settings);
  }

  /**
   * Does nothing because no operations are necessary before inserting an entry.
   */
  @Override
  protected void preInsert(MTreeEntry entry) {
    // do nothing
  }

  /**
   * @return a new MTreeDirectoryEntry representing the specified node
   */
  @Override
  protected MTreeEntry createNewDirectoryEntry(MTreeNode<O> node, DBID routingObjectID, double parentDistance) {
    return new MTreeDirectoryEntry(routingObjectID, parentDistance, node.getPageID(), node.coveringRadiusFromEntries(routingObjectID, this));
  }

  /**
   * @return a new MTreeDirectoryEntry by calling
   *         <code>new MTreeDirectoryEntry<D>(null, null, 0, null)</code>
   */
  @Override
  protected MTreeEntry createRootEntry() {
    return new MTreeDirectoryEntry(null, 0., 0, 0.);
  }

  /**
   * @return a new MTreeNode which is a leaf node
   */
  @Override
  protected MTreeNode<O> createNewLeafNode() {
    return new MTreeNode<>(leafCapacity, true);
  }

  /**
   * @return a new MTreeNode which is a directory node
   */
  @Override
  protected MTreeNode<O> createNewDirectoryNode() {
    return new MTreeNode<>(dirCapacity, false);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
