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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.NodeArrayAdapter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract superclass for index structures based on a R*-Tree.
 *
 * Implementation Note: The restriction on NumberVector (as opposed to e.g.
 * FeatureVector) is intentional, because we have spatial requirements.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @navhas - contains - AbstractRStarTreeNode
 * @composed - - - RTreeSettings
 * @composed - - - Statistics
 *
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractRStarTree<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry, S extends RTreeSettings> extends SpatialIndexTree<N, E> {
  /**
   * Development flag: This will enable some extra integrity checks on the tree.
   */
  protected static final boolean EXTRA_INTEGRITY_CHECKS = false;

  /**
   * The height of this R*-Tree.
   */
  protected int height;

  /**
   * For counting the number of distance computations.
   */
  public Statistics statistics = new Statistics();

  /**
   * The last inserted entry.
   */
  E lastInsertedEntry = null;

  /**
   * Settings class.
   */
  protected S settings;

  /**
   * Constructor.
   *
   * @param pagefile Page file
   * @param settings Settings
   */
  public AbstractRStarTree(PageFile<N> pagefile, S settings) {
    super(pagefile);
    this.settings = settings;
  }

  /**
   * Returns the path to the leaf entry in the specified subtree that represents
   * the data object with the specified mbr and id.
   *
   * @param subtree the subtree to be tested
   * @param mbr the mbr to look for
   * @param id the id to look for
   * @return the path to the leaf entry of the specified subtree that represents
   *         the data object with the specified mbr and id
   */
  protected IndexTreePath<E> findPathToObject(IndexTreePath<E> subtree, SpatialComparable mbr, DBIDRef id) {
    N node = getNode(subtree.getEntry());
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        if(DBIDUtil.equal(((LeafEntry) node.getEntry(i)).getDBID(), id)) {
          return new IndexTreePath<>(subtree, node.getEntry(i), i);
        }
      }
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        if(SpatialUtil.intersects(node.getEntry(i), mbr)) {
          IndexTreePath<E> childSubtree = new IndexTreePath<>(subtree, node.getEntry(i), i);
          IndexTreePath<E> path = findPathToObject(childSubtree, mbr, id);
          if(path != null) {
            return path;
          }
        }
      }
    }
    return null;
  }

  @Override
  public void insertLeaf(E leaf) {
    if(!initialized) {
      initialize(leaf);
    }
    settings.getOverflowTreatment().reinitialize();

    preInsert(leaf);
    insertLeafEntry(leaf);

    doExtraIntegrityChecks();
  }

  /**
   * Inserts the specified leaf entry into this R*-Tree.
   *
   * @param entry the leaf entry to be inserted
   */
  protected void insertLeafEntry(E entry) {
    lastInsertedEntry = entry;
    // choose subtree for insertion
    IndexTreePath<E> subtree = choosePath(getRootPath(), entry, height, 1);

    if(getLogger().isDebugging()) {
      getLogger().debugFine("insertion-subtree " + subtree);
    }

    N parent = getNode(subtree.getEntry());
    parent.addLeafEntry(entry);
    writeNode(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);
  }

  /**
   * Inserts the specified directory entry at the specified level into this
   * R*-Tree.
   *
   * @param entry the directory entry to be inserted
   * @param depth the depth at which the directory entry is to be inserted
   */
  protected void insertDirectoryEntry(E entry, int depth) {
    lastInsertedEntry = entry;
    // choose node for insertion of o
    IndexTreePath<E> subtree = choosePath(getRootPath(), entry, depth, 1);
    if(getLogger().isDebugging()) {
      getLogger().debugFine("subtree " + subtree);
    }

    N parent = getNode(subtree.getEntry());
    parent.addDirectoryEntry(entry);
    writeNode(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);
  }

  /**
   * Delete a leaf at a given path - deletions for non-leaves are not supported!
   *
   * @param deletionPath Path to delete
   */
  protected void deletePath(IndexTreePath<E> deletionPath) {
    N leaf = getNode(deletionPath.getParentPath().getEntry());
    int index = deletionPath.getIndex();

    // delete o
    E entry = leaf.getEntry(index);
    leaf.deleteEntry(index);
    writeNode(leaf);

    // condense the tree
    Stack<N> stack = new Stack<>();
    condenseTree(deletionPath.getParentPath(), stack);

    // reinsert underflow nodes
    while(!stack.empty()) {
      N node = stack.pop();
      if(node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          settings.getOverflowTreatment().reinitialize(); // Intended?
          this.insertLeafEntry(node.getEntry(i));
        }
      }
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          stack.push(getNode(node.getEntry(i)));
        }
      }
      deleteNode(node);
    }
    postDelete(entry);

    doExtraIntegrityChecks();
  }

  /**
   * Initializes this R*-Tree from an existing persistent file.
   *
   * {@inheritDoc}
   */
  @Override
  public void initializeFromFile(TreeIndexHeader header, PageFile<N> file) {
    super.initializeFromFile(header, file);
    // compute height
    this.height = computeHeight();

    if(getLogger().isDebugging()) {
      getLogger().debugFine(new StringBuilder(100).append(getClass()) //
          .append("\n height = ").append(height).toString());
    }
  }

  @Override
  protected void initializeCapacities(E exampleLeaf) {
    /* Simulate the creation of a leaf page to get the page capacity */
    try {
      int cap = 0;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      SpatialPointLeafEntry sl = new SpatialPointLeafEntry(DBIDUtil.importInteger(0), new double[exampleLeaf.getDimensionality()]);
      while(baos.size() <= getPageSize()) {
        sl.writeExternal(oos);
        oos.flush();
        cap++;
      }
      // the last one caused the page to overflow.
      leafCapacity = cap - 1;
    }
    catch(IOException e) {
      throw new AbortException("Error determining page sizes.", e);
    }

    /* Simulate the creation of a directory page to get the capacity */
    try {
      int cap = 0;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      ModifiableHyperBoundingBox hb = new ModifiableHyperBoundingBox(new double[exampleLeaf.getDimensionality()], new double[exampleLeaf.getDimensionality()]);
      SpatialDirectoryEntry sl = new SpatialDirectoryEntry(0, hb);
      while(baos.size() <= getPageSize()) {
        sl.writeExternal(oos);
        oos.flush();
        cap++;
      }
      dirCapacity = cap - 1;
    }
    catch(IOException e) {
      throw new AbortException("Error determining page sizes.", e);
    }

    if(dirCapacity <= 2) {
      throw new IllegalArgumentException("Node size of " + getPageSize() + " bytes is chosen too small!");
    }

    final Logging log = getLogger();
    if(dirCapacity < 10) {
      log.warning("Page size is choosen very small! Maximum number of entries in a directory node = " + dirCapacity);
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.floor(dirCapacity * settings.relativeMinFill);
    if(dirMinimum < 1) {
      dirMinimum = 1;
    }

    if(leafCapacity <= 2) {
      throw new IllegalArgumentException("Node size of " + getPageSize() + " bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      log.warning("Page size is choosen very small! Maximum number of entries in a leaf node = " + leafCapacity);
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.floor(leafCapacity * settings.relativeMinFill);
    if(leafMinimum < 1) {
      leafMinimum = 1;
    }
  }

  /**
   * Test whether a bulk insert is still possible.
   *
   * @return Success code
   */
  public boolean canBulkLoad() {
    return (settings.bulkSplitter != null && !initialized);
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   *
   * @param objects the objects to be inserted
   * @return the array of leaf nodes containing the objects
   */
  protected List<E> createBulkLeafNodes(List<E> objects) {
    int minEntries = leafMinimum;
    int maxEntries = leafCapacity;

    ArrayList<E> result = new ArrayList<>();
    List<List<E>> partitions = settings.bulkSplitter.partition(objects, minEntries, maxEntries);

    for(List<E> partition : partitions) {
      // create leaf node
      N leafNode = createNewLeafNode();

      // insert data
      for(E o : partition) {
        leafNode.addLeafEntry(o);
      }
      // write to file
      writeNode(leafNode);

      result.add(createNewDirectoryEntry(leafNode));

      if(getLogger().isDebugging()) {
        getLogger().debugFine("Created leaf page " + leafNode.getPageID());
      }
    }

    if(getLogger().isDebugging()) {
      getLogger().debugFine("numDataPages = " + result.size());
    }
    return result;
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor.
   *
   * @param entries Entries to bulk load
   */
  protected abstract void bulkLoad(List<E> entries);

  /**
   * Returns the height of this R*-Tree.
   *
   * @return the height of this R*-Tree
   */
  public final int getHeight() {
    return height;
  }

  /**
   * Sets the height of this R*-Tree.
   *
   * @param height the height to be set
   */
  protected void setHeight(int height) {
    this.height = height;
  }

  /**
   * Computes the height of this RTree. Is called by the constructor.
   *
   * @return the height of this RTree
   */
  protected abstract int computeHeight();

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  protected abstract boolean hasOverflow(N node);

  /**
   * Returns true if in the specified node an underflow occurred, false
   * otherwise.
   *
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false
   *         otherwise
   */
  protected abstract boolean hasUnderflow(N node);

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   * @return the newly created directory entry
   */
  protected abstract E createNewDirectoryEntry(N node);

  /**
   * Creates a new root node that points to the two specified child nodes and
   * return the path to the new root.
   *
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return the path to the new root node that points to the two specified
   *         child nodes
   */
  protected IndexTreePath<E> createNewRoot(final N oldRoot, final N newNode) {
    N root = createNewDirectoryNode();
    writeNode(root);

    // switch the ids
    oldRoot.setPageID(root.getPageID());
    if(!oldRoot.isLeaf()) {
      for(int i = 0; i < oldRoot.getNumEntries(); i++) {
        writeNode(getNode(oldRoot.getEntry(i)));
      }
    }

    root.setPageID(getRootID());
    root.addDirectoryEntry(createNewDirectoryEntry(oldRoot));
    root.addDirectoryEntry(createNewDirectoryEntry(newNode));

    writeNode(root);
    writeNode(oldRoot);
    writeNode(newNode);
    if(getLogger().isDebugging()) {
      getLogger().debugFine("Create new Root: ID=" + root.getPageID() + "\nchild1 " + oldRoot + " " + new HyperBoundingBox(createNewDirectoryEntry(oldRoot)) + "\nchild2 " + newNode + " " + new HyperBoundingBox(createNewDirectoryEntry(newNode)));
    }

    return new IndexTreePath<>(null, getRootEntry(), -1);
  }

  /**
   * Test on whether or not any child of <code>node</code> contains
   * <code>mbr</code>. If there are several containing children, the child with
   * the minimum volume is chosen in order to get compact pages.
   *
   * @param node subtree
   * @param mbr MBR to test for
   * @return the child of <code>node</code> containing <code>mbr</code> with the
   *         minimum volume or <code>null</code> if none exists
   */
  protected IndexTreePath<E> containedTest(IndexTreePath<E> subtree, N node, SpatialComparable mbr) {
    E containingEntry = null;
    int index = -1;
    double cEVol = Double.NaN;
    for(int i = 0; i < node.getNumEntries(); i++) {
      E ei = node.getEntry(i);
      // skip test on pairwise overlaps
      if(SpatialUtil.contains(ei, mbr)) {
        if(containingEntry == null) {
          containingEntry = ei;
          index = i;
        }
        else {
          double tempVol = SpatialUtil.volume(ei);
          if(Double.isNaN(cEVol)) { // calculate volume of currently best
            cEVol = SpatialUtil.volume(containingEntry);
          }
          // take containing node with lowest volume
          if(tempVol < cEVol) {
            cEVol = tempVol;
            containingEntry = ei;
            index = i;
          }
        }
      }
    }
    return (containingEntry == null ? null : new IndexTreePath<>(subtree, containingEntry, index));
  }

  /**
   * Chooses the best path of the specified subtree for insertion of the given
   * mbr at the specified level.
   *
   * @param subtree the subtree to be tested for insertion
   * @param mbr the mbr to be inserted
   * @param depth Reinsertion depth, 1 indicates root level
   * @param cur Current depth
   * @return the path of the appropriate subtree to insert the given mbr
   */
  protected IndexTreePath<E> choosePath(IndexTreePath<E> subtree, SpatialComparable mbr, int depth, int cur) {
    if(getLogger().isDebuggingFiner()) {
      getLogger().debugFiner("node " + subtree + ", depth " + depth);
    }

    N node = getNode(subtree.getEntry());
    if(node == null) {
      throw new RuntimeException("Page file did not return node for node id: " + getPageID(subtree.getEntry()));
    }
    if(node.isLeaf()) {
      return subtree;
    }
    // first test on containment
    IndexTreePath<E> newSubtree = containedTest(subtree, node, mbr);
    if(newSubtree != null) {
      return (++cur == depth) ? newSubtree : choosePath(newSubtree, mbr, depth, cur);
    }

    N childNode = getNode(node.getEntry(0));
    int num = settings.insertionStrategy.choose(node, NodeArrayAdapter.STATIC, mbr, height, cur);
    newSubtree = new IndexTreePath<>(subtree, node.getEntry(num), num);
    ++cur;
    if(cur == depth) {
      return newSubtree;
    }
    // children are leafs
    if(childNode.isLeaf()) {
      assert cur == newSubtree.getPathCount(); // Check for programming errors
      throw new IllegalArgumentException("childNode is leaf, but currentDepth != depth: " + cur + " != " + depth);
    }
    // children are directory nodes
    return choosePath(newSubtree, mbr, depth, cur);
  }

  /**
   * Treatment of overflow in the specified node: if the node is not the root
   * node and this is the first call of overflowTreatment in the given level
   * during insertion the specified node will be reinserted, otherwise the node
   * will be split.
   *
   * @param node the node where an overflow occurred
   * @param path the path to the specified node
   * @return the newly created split node in case of split, null in case of
   *         reinsertion
   */
  private N overflowTreatment(N node, IndexTreePath<E> path) {
    if(settings.getOverflowTreatment().handleOverflow(this, node, path)) {
      return null;
    }
    return split(node);
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be split
   * @return the newly created split node
   */
  private N split(N node) {
    // choose the split dimension and the split point
    int minimum = node.isLeaf() ? leafMinimum : dirMinimum;
    long[] split = settings.nodeSplitter.split(node, NodeArrayAdapter.STATIC, minimum);

    // New node
    final N newNode = node.isLeaf() ? createNewLeafNode() : createNewDirectoryNode();
    // do the split
    node.splitByMask(newNode, split);

    // write changes to file
    writeNode(node);
    writeNode(newNode);

    return newNode;
  }

  /**
   * Reinserts the specified node at the specified level.
   *
   * @param node the node to be reinserted
   * @param path the path to the node
   * @param offs the nodes indexes to reinsert
   */
  public void reInsert(N node, IndexTreePath<E> path, int[] offs) {
    final int depth = path.getPathCount();

    long[] remove = BitsUtil.zero(node.getCapacity());
    List<E> reInsertEntries = new ArrayList<>(offs.length);
    for(int i = 0; i < offs.length; i++) {
      reInsertEntries.add(node.getEntry(offs[i]));
      BitsUtil.setI(remove, offs[i]);
    }
    // Remove the entries we reinsert
    node.removeMask(remove);
    writeNode(node);

    // and adapt the mbrs
    IndexTreePath<E> childPath = path;
    N child = node;
    while(childPath.getParentPath() != null) {
      N parent = getNode(childPath.getParentPath().getEntry());
      int indexOfChild = childPath.getIndex();
      if(child.adjustEntry(parent.getEntry(indexOfChild))) {
        writeNode(parent);
        childPath = childPath.getParentPath();
        child = parent;
      }
      else {
        break;
        // TODO: stop writing when MBR didn't change!
      }
    }

    // reinsert the first entries
    final Logging log = getLogger();
    for(E entry : reInsertEntries) {
      if(node.isLeaf()) {
        if(log.isDebugging()) {
          log.debug("reinsert " + entry);
        }
        insertLeafEntry(entry);
      }
      else {
        if(log.isDebugging()) {
          log.debug("reinsert " + entry + " at " + depth);
        }
        insertDirectoryEntry(entry, depth);
      }
    }
  }

  /**
   * Adjusts the tree after insertion of some nodes.
   *
   * @param subtree the subtree to be adjusted
   */
  protected void adjustTree(IndexTreePath<E> subtree) {
    final Logging log = getLogger();
    if(log.isDebugging()) {
      log.debugFine("Adjust tree " + subtree);
    }

    // get the root of the subtree
    N node = getNode(subtree.getEntry());

    // overflow in node
    if(hasOverflow(node)) {
      // treatment of overflow: reinsertion or split
      N split = overflowTreatment(node, subtree);

      // node was split
      if(split != null) {
        // if root was split: create a new root that points the two
        // split nodes
        if(isRoot(node)) {
          IndexTreePath<E> newRootPath = createNewRoot(node, split);
          height++;
          adjustTree(newRootPath);
        }
        // node is not root
        else {
          // get the parent and add the new split node
          N parent = getNode(subtree.getParentPath().getEntry());
          if(log.isDebugging()) {
            log.debugFine("parent " + parent);
          }
          parent.addDirectoryEntry(createNewDirectoryEntry(split));

          // adjust the entry representing the (old) node, that has
          // been split

          // This does not work in the persistent version
          // node.adjustEntry(subtree.getEntry());
          node.adjustEntry(parent.getEntry(subtree.getIndex()));

          // write changes in parent to file
          writeNode(parent);
          adjustTree(subtree.getParentPath());
        }
      }
    }
    // no overflow, only adjust parameters of the entry representing the
    // node
    else {
      if(!isRoot(node)) {
        N parent = getNode(subtree.getParentPath().getEntry());
        E entry = parent.getEntry(subtree.getIndex());
        boolean changed = node.adjustEntryIncremental(entry, lastInsertedEntry);
        if(changed) {
          // node.adjustEntry(parent.getEntry(index));
          // write changes in parent to file
          writeNode(parent);
          adjustTree(subtree.getParentPath());
        }
      }
      // root level is reached
      else {
        node.adjustEntry(getRootEntry());
      }
    }
  }

  /**
   * Condenses the tree after deletion of some nodes.
   *
   * @param subtree the subtree to be condensed
   * @param stack the stack holding the nodes to be reinserted after the tree
   *        has been condensed
   */
  private void condenseTree(IndexTreePath<E> subtree, Stack<N> stack) {
    N node = getNode(subtree.getEntry());
    // node is not root
    if(!isRoot(node)) {
      N parent = getNode(subtree.getParentPath().getEntry());
      int index = subtree.getIndex();
      if(hasUnderflow(node)) {
        if(parent.deleteEntry(index)) {
          stack.push(node);
        }
        else {
          node.adjustEntry(parent.getEntry(index));
        }
      }
      else {
        node.adjustEntry(parent.getEntry(index));
      }
      writeNode(parent);
      // get subtree to parent
      condenseTree(subtree.getParentPath(), stack);
    }

    // node is root
    else {
      if(hasUnderflow(node) && node.getNumEntries() == 1 && !node.isLeaf()) {
        N child = getNode(node.getEntry(0));
        final N newRoot;
        if(child.isLeaf()) {
          newRoot = createNewLeafNode();
          newRoot.setPageID(getRootID());
          for(int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addLeafEntry(child.getEntry(i));
          }
        }
        else {
          newRoot = createNewDirectoryNode();
          newRoot.setPageID(getRootID());
          for(int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addDirectoryEntry(child.getEntry(i));
          }
        }
        writeNode(newRoot);
        height--;
      }
    }
  }

  @Override
  public final List<E> getLeaves() {
    List<E> result = new ArrayList<>();
    if(height == 1) {
      result.add(getRootEntry());
      return result;
    }

    getLeafNodes(getRoot(), result, height);
    return result;
  }

  /**
   * Determines the entries pointing to the leaf nodes of the specified subtree.
   *
   * @param node the subtree
   * @param result the result to store the ids in
   * @param currentLevel the level of the node in the R-Tree
   */
  private void getLeafNodes(N node, List<E> result, int currentLevel) {
    // Level 1 are the leaf nodes, Level 2 is the one atop!
    if(currentLevel == 2) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        result.add(node.getEntry(i));
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        getLeafNodes(getNode(node.getEntry(i)), result, (currentLevel - 1));
      }
    }
  }

  /**
   * Perform additional integrity checks.
   */
  public void doExtraIntegrityChecks() {
    if(EXTRA_INTEGRITY_CHECKS) {
      getRoot().integrityCheck(this);
    }
  }

  @Override
  public void logStatistics() {
    Logging log = getLogger();
    if(log.isStatistics()) {
      super.logStatistics();
      log.statistics(new LongStatistic(this.getClass().getName() + ".height", height));
      statistics.logStatistics();
    }
  }

  /**
   * Class for tracking some statistics.
   *
   * @author Erich Schubert
   *
   * @composed - - - Counter
   */
  public class Statistics {
    /**
     * For counting the number of distance computations.
     */
    protected final Counter distanceCalcs;

    /**
     * For counting the number of knn queries answered.
     */
    protected final Counter knnQueries;

    /**
     * For counting the number of range queries answered.
     */
    protected final Counter rangeQueries;

    /**
     * Constructor.
     */
    public Statistics() {
      super();
      Logging log = getLogger();
      final String prefix = AbstractRStarTree.this.getClass().getName();
      distanceCalcs = log.isStatistics() ? log.newCounter(prefix + ".distancecalcs") : null;
      knnQueries = log.isStatistics() ? log.newCounter(prefix + ".knnqueries") : null;
      rangeQueries = log.isStatistics() ? log.newCounter(prefix + ".rangequeries") : null;
    }

    /**
     * Count a distance computation.
     */
    public void countDistanceCalculation() {
      if(distanceCalcs != null) {
        distanceCalcs.increment();
      }
    }

    /**
     * Count a knn query invocation.
     */
    public void countKNNQuery() {
      if(knnQueries != null) {
        knnQueries.increment();
      }
    }

    /**
     * Count a range query invocation.
     */
    public void countRangeQuery() {
      if(rangeQueries != null) {
        rangeQueries.increment();
      }
    }

    /**
     * Log the statistics.
     */
    public void logStatistics() {
      Logging log = getLogger();
      if(statistics.distanceCalcs != null) {
        log.statistics(statistics.distanceCalcs);
      }
      if(statistics.knnQueries != null) {
        log.statistics(statistics.knnQueries);
      }
      if(statistics.rangeQueries != null) {
        log.statistics(statistics.rangeQueries);
      }
    }
  }

  /**
   * Returns a string representation of this R*-Tree.
   *
   * @return a string representation of this R*-Tree
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    if(initialized) {
      N node = getRoot();
      int dim = getRootEntry().getDimensionality();

      while(!node.isLeaf()) {
        if(node.getNumEntries() > 0) {
          E entry = node.getEntry(0);
          node = getNode(entry);
          levels++;
        }
      }

      BreadthFirstEnumeration<N, E> enumeration = new BreadthFirstEnumeration<>(this, getRootPath());
      while(enumeration.hasNext()) {
        IndexTreePath<E> indexPath = enumeration.next();
        E entry = indexPath.getEntry();
        if(entry instanceof LeafEntry) {
          objects++;
        }
        else {
          node = getNode(entry);
          if(node.isLeaf()) {
            leafNodes++;
          }
          else {
            dirNodes++;
          }
        }
      }
      result.append(getClass().getName()).append(" has ").append((levels + 1)).append(" levels.\n") //
          .append(dirNodes).append(" Directory Knoten (max = ").append(dirCapacity).append(", min = ").append(dirMinimum).append(")\n") //
          .append(leafNodes).append(" Daten Knoten (max = ").append(leafCapacity).append(", min = ").append(leafMinimum).append(")\n") //
          .append(objects).append(' ').append(dim).append("-dim. Punkte im Baum \n");
      // PageFileUtil.appendPageFileStatistics(result, getPageFileStatistics());
    }
    else {
      result.append(getClass().getName()).append(" is empty!\n");
    }

    return result.toString();
  }
}
