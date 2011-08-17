package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MTreeNode oneway - - contains
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 */
@Title("M-Tree")
@Description("Efficient Access Method for Similarity Search in Metric Spaces")
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", url = "http://www.vldb.org/conf/1997/P426.PDF")
public class MTree<O, D extends Distance<D>> extends AbstractMTree<O, D, MTreeNode<O, D>, MTreeEntry<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MTree.class);

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public MTree(PageFile<MTreeNode<O, D>> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(pagefile, distanceQuery, distanceFunction);
  }

  /**
   * Does nothing because no operations are necessary before inserting an entry.
   */
  @Override
  protected void preInsert(@SuppressWarnings("unused") MTreeEntry<D> entry) {
    // do nothing
  }

  @Override
  protected void initializeCapacities(MTreeEntry<D> exampleLeaf) {
    int distanceSize = exampleLeaf.getParentDistance().externalizableSize();

    // FIXME: simulate a proper feature size!
    int featuresize = 0; // DatabaseUtil.dimensionality(relation);

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance) + 1
    // dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize +
    // distanceSize) + 1;

    // dirCapacity = (pageSize - overhead) / (nodeID + **object feature size** +
    // coveringRadius + parentDistance) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + featuresize + distanceSize + distanceSize) + 1;

    if(dirCapacity <= 2) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }
    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance) +
    // 1
    // leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize) + 1;
    // leafCapacity = (pageSize - overhead) / (objectID + ** object size ** +
    // parentDistance) +
    // 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + featuresize + distanceSize) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    if(logger.isVerbose()) {
      logger.verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
    }
  }

  /**
   * @return a new MTreeDirectoryEntry representing the specified node
   */
  @Override
  protected MTreeEntry<D> createNewDirectoryEntry(MTreeNode<O, D> node, DBID routingObjectID, D parentDistance) {
    return new MTreeDirectoryEntry<D>(routingObjectID, parentDistance, node.getPageID(), node.coveringRadius(routingObjectID, this));
  }

  /**
   * @return a new MTreeDirectoryEntry by calling
   *         <code>new MTreeDirectoryEntry<D>(null, null, 0, null)</code>
   */
  @Override
  protected MTreeEntry<D> createRootEntry() {
    return new MTreeDirectoryEntry<D>(null, null, 0, null);
  }

  /**
   * @return a new MTreeNode which is a leaf node
   */
  @Override
  protected MTreeNode<O, D> createNewLeafNode() {
    return new MTreeNode<O, D>(leafCapacity, true);
  }

  /**
   * @return a new MTreeNode which is a directory node
   */
  @Override
  protected MTreeNode<O, D> createNewDirectoryNode() {
    return new MTreeNode<O, D>(dirCapacity, false);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}