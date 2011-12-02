package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.LeastOverlapInsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.OverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.SplitStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.TopologicalSplitter;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract superclass for index structures based on a R*-Tree.
 * 
 * Implementation Note: The restriction on NumberVector (as opposed to e.g.
 * FeatureVector) is intentional, because we have spatial requirements.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.has AbstractRStarTreeNode oneway - - contains
 * @apiviz.uses Enlargement
 * @apiviz.composedOf BulkSplit
 * @apiviz.composedOf SplitStrategy
 * @apiviz.composedOf InsertionStrategy
 * 
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractRStarTree<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends SpatialIndexTree<N, E> {
  /**
   * Development flag: This will enable some extra integrity checks on the tree.
   */
  protected final static boolean extraIntegrityChecks = false;

  /**
   * The height of this R*-Tree.
   */
  protected int height;

  /**
   * For counting the number of distance computations.
   */
  public int distanceCalcs = 0;

  /**
   * The last inserted entry
   */
  E lastInsertedEntry = null;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit bulkSplitter;

  /**
   * The split strategy
   */
  protected SplitStrategy nodeSplitter = TopologicalSplitter.STATIC;

  /**
   * The insertion strategy to use
   */
  protected InsertionStrategy insertionStrategy = LeastOverlapInsertionStrategy.STATIC;

  /**
   * Overflow treatment
   */
  protected OverflowTreatment overflowTreatment = LimitedReinsertOverflowTreatment.RSTAR_OVERFLOW;

  /**
   * Relative minimum fill
   */
  protected double relativeMinFill = 0.4;

  /**
   * Constructor
   * 
   * @param pagefile Page file
   */
  public AbstractRStarTree(PageFile<N> pagefile) {
    super(pagefile);
  }

  /**
   * Set the bulk loading strategy
   * 
   * @param bulkSplitter Bulk loading strategy
   */
  public void setBulkStrategy(BulkSplit bulkSplitter) {
    this.bulkSplitter = bulkSplitter;
  }

  /**
   * Set the node splitting strategy.
   * 
   * @param nodeSplitter the split strategy to set
   */
  public void setNodeSplitStrategy(SplitStrategy nodeSplitter) {
    if(nodeSplitter != null) {
      this.nodeSplitter = nodeSplitter;
    }
    else {
      getLogger().warning("Ignoring setNodeSplitStrategy(null)");
    }
  }

  /**
   * Set insertion strategy
   * 
   * @param insertionStrategy the insertion strategy to set
   */
  public void setInsertionStrategy(InsertionStrategy insertionStrategy) {
    if(insertionStrategy != null) {
      this.insertionStrategy = insertionStrategy;
    }
    else {
      getLogger().warning("Ignoring setInsertionStrategy(null)");
    }
  }

  /**
   * Set the overflow treatment strategy.
   * 
   * @param overflowTreatment overflow treatment strategy
   */
  public void setOverflowTreatment(OverflowTreatment overflowTreatment) {
    this.overflowTreatment = overflowTreatment;
  }

  /**
   * Set the relative minimum fill. (Only supported before the tree was used!)
   * 
   * @param relative Relative minimum fill
   */
  public void setMinimumFill(double relative) {
    this.relativeMinFill = relative;
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
  protected IndexTreePath<E> findPathToObject(IndexTreePath<E> subtree, SpatialComparable mbr, DBID id) {
    N node = getNode(subtree.getLastPathComponent().getEntry());
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        if(((LeafEntry) node.getEntry(i)).getDBID().equals(id)) {
          return subtree.pathByAddingChild(new TreeIndexPathComponent<E>(node.getEntry(i), i));
        }
      }
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        if(SpatialUtil.intersects(node.getEntry(i), mbr)) {
          IndexTreePath<E> childSubtree = subtree.pathByAddingChild(new TreeIndexPathComponent<E>(node.getEntry(i), i));
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
    overflowTreatment.reinitialize();

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
    IndexTreePath<E> subtree = choosePath(getRootPath(), entry, 1);

    if(getLogger().isDebugging()) {
      getLogger().debugFine("insertion-subtree " + subtree);
    }

    N parent = getNode(subtree.getLastPathComponent().getEntry());
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
   * @param level the level at which the directory entry is to be inserted
   */
  protected void insertDirectoryEntry(E entry, int level) {
    lastInsertedEntry = entry;
    // choose node for insertion of o
    IndexTreePath<E> subtree = choosePath(getRootPath(), entry, level);
    if(getLogger().isDebugging()) {
      getLogger().debugFine("subtree " + subtree);
    }

    N parent = getNode(subtree.getLastPathComponent().getEntry());
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
    N leaf = getNode(deletionPath.getParentPath().getLastPathComponent().getEntry());
    int index = deletionPath.getLastPathComponent().getIndex();

    // delete o
    E entry = leaf.getEntry(index);
    leaf.deleteEntry(index);
    writeNode(leaf);

    // condense the tree
    Stack<N> stack = new Stack<N>();
    condenseTree(deletionPath.getParentPath(), stack);

    // reinsert underflow nodes
    while(!stack.empty()) {
      N node = stack.pop();
      if(node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          overflowTreatment.reinitialize(); // Intended?
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
   */
  @Override
  public void initializeFromFile(TreeIndexHeader header, PageFile<N> file) {
    super.initializeFromFile(header, file);
    // compute height
    this.height = computeHeight();

    if(getLogger().isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass());
      msg.append("\n height = ").append(height);
      getLogger().debugFine(msg.toString());
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

    if(dirCapacity <= 1) {
      throw new IllegalArgumentException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      getLogger().warning("Page size is choosen very small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * relativeMinFill);
    if(dirMinimum < 2) {
      dirMinimum = 2;
    }

    if(leafCapacity <= 1) {
      throw new IllegalArgumentException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      getLogger().warning("Page size is choosen very small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * relativeMinFill);
    if(leafMinimum < 2) {
      leafMinimum = 2;
    }

    if(getLogger().isVerbose()) {
      getLogger().verbose("Directory Capacity:  " + (dirCapacity - 1) + "\nDirectory minimum: " + dirMinimum + "\nLeaf Capacity:     " + (leafCapacity - 1) + "\nLeaf Minimum:      " + leafMinimum);
    }
  }

  /**
   * Test whether a bulk insert is still possible.
   * 
   * @return Success code
   */
  public boolean canBulkLoad() {
    return (bulkSplitter != null && !initialized);
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   * 
   * @param objects the objects to be inserted
   * @return the array of leaf nodes containing the objects
   */
  protected List<N> createBulkLeafNodes(List<E> objects) {
    int minEntries = leafMinimum;
    int maxEntries = leafCapacity - 1;

    ArrayList<N> result = new ArrayList<N>();
    List<List<E>> partitions = bulkSplitter.partition(objects, minEntries, maxEntries);

    for(List<E> partition : partitions) {
      // create leaf node
      N leafNode = createNewLeafNode();
      result.add(leafNode);

      // insert data
      for(E o : partition) {
        leafNode.addLeafEntry(o);
      }

      // write to file
      writeNode(leafNode);

      if(getLogger().isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("pageNo ").append(leafNode.getPageID()).append("\n");
        getLogger().debugFine(msg.toString());
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
   */
  abstract protected void bulkLoad(List<E> entrys);

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
  abstract protected int computeHeight();

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  abstract protected boolean hasOverflow(N node);

  /**
   * Returns true if in the specified node an underflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false
   *         otherwise
   */
  abstract protected boolean hasUnderflow(N node);

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   * @return the newly created directory entry
   */
  abstract protected E createNewDirectoryEntry(N node);

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
        N node = getNode(oldRoot.getEntry(i));
        writeNode(node);
      }
    }

    root.setPageID(getRootID());
    E oldRootEntry = createNewDirectoryEntry(oldRoot);
    E newNodeEntry = createNewDirectoryEntry(newNode);
    root.addDirectoryEntry(oldRootEntry);
    root.addDirectoryEntry(newNodeEntry);

    writeNode(root);
    writeNode(oldRoot);
    writeNode(newNode);
    if(getLogger().isDebugging()) {
      String msg = "Create new Root: ID=" + root.getPageID();
      msg += "\nchild1 " + oldRoot + " " + new HyperBoundingBox(oldRoot) + " " + new HyperBoundingBox(oldRootEntry);
      msg += "\nchild2 " + newNode + " " + new HyperBoundingBox(newNode) + " " + new HyperBoundingBox(newNodeEntry);
      msg += "\n";
      getLogger().debugFine(msg);
    }

    return new IndexTreePath<E>(new TreeIndexPathComponent<E>(getRootEntry(), null));
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
  protected TreeIndexPathComponent<E> containedTest(N node, SpatialComparable mbr) {
    E containingEntry = null;
    int index = -1;
    double cEVol = Double.NaN;
    E ei;
    for(int i = 0; i < node.getNumEntries(); i++) {
      ei = node.getEntry(i);
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
    return (containingEntry == null ? null : new TreeIndexPathComponent<E>(containingEntry, index));
  }

  /**
   * Chooses the best path of the specified subtree for insertion of the given
   * mbr at the specified level.
   * 
   * @param subtree the subtree to be tested for insertion
   * @param mbr the mbr to be inserted
   * @param level the level at which the mbr should be inserted (level 1
   *        indicates leaf-level)
   * @return the path of the appropriate subtree to insert the given mbr
   */
  protected IndexTreePath<E> choosePath(IndexTreePath<E> subtree, SpatialComparable mbr, int level) {
    if(getLogger().isDebuggingFiner()) {
      getLogger().debugFiner("node " + subtree + ", level " + level);
    }

    N node = getNode(subtree.getLastPathComponent().getEntry());
    if(node == null) {
      throw new RuntimeException("Page file did not return node for node id: " + getPageID(subtree.getLastPathComponent().getEntry()));
    }
    if(node.isLeaf()) {
      return subtree;
    }
    // first test on containment
    TreeIndexPathComponent<E> containingEntry = containedTest(node, mbr);
    if(containingEntry != null) {
      IndexTreePath<E> newSubtree = subtree.pathByAddingChild(containingEntry);
      if(height - subtree.getPathCount() == level) {
        return newSubtree;
      }
      else {
        return choosePath(newSubtree, mbr, level);
      }
    }

    N childNode = getNode(node.getEntry(0));
    final List<E> entries = node.getEntries();
    int num = insertionStrategy.choose(entries, ArrayLikeUtil.listAdapter(entries), mbr, height, subtree.getPathCount());
    TreeIndexPathComponent<E> comp = new TreeIndexPathComponent<E>(entries.get(num), num);
    // children are leafs
    if(childNode.isLeaf()) {
      if(height - subtree.getPathCount() == level) {
        return subtree.pathByAddingChild(comp);
      }
      else {
        throw new IllegalArgumentException("childNode is leaf, but currentLevel != level: " + (height - subtree.getPathCount()) + " != " + level);
      }
    }
    // children are directory nodes
    else {
      IndexTreePath<E> newSubtree = subtree.pathByAddingChild(comp);
      // desired level is reached
      if(height - subtree.getPathCount() == level) {
        return newSubtree;
      }
      else {
        return choosePath(newSubtree, mbr, level);
      }
    }
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
    if(overflowTreatment.handleOverflow(this, node, path)) {
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
    final List<E> entries = node.getEntries();
    BitSet split = nodeSplitter.split(entries, ArrayLikeUtil.listAdapter(entries), minimum);

    // New node
    final N newNode;
    if(node.isLeaf()) {
      newNode = createNewLeafNode();
    }
    else {
      newNode = createNewDirectoryNode();
    }
    // do the split
    node.splitTo(newNode, entries, split);

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
    final int level = height - (path.getPathCount() - 1);

    BitSet remove = new BitSet();
    List<E> entries = node.getEntries();
    List<E> reInsertEntries = new ArrayList<E>(offs.length);
    for(int i = 0; i < offs.length; i++) {
      reInsertEntries.add(entries.get(offs[i]));
      remove.set(offs[i]);
    }
    // Remove the entries we reinsert
    node.removeMask(remove);
    writeNode(node);

    // and adapt the mbrs
    IndexTreePath<E> childPath = path;
    N child = node;
    while(childPath.getParentPath() != null) {
      N parent = getNode(childPath.getParentPath().getLastPathComponent().getEntry());
      int indexOfChild = childPath.getLastPathComponent().getIndex();
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
    for(E entry : reInsertEntries) {
      if(node.isLeaf()) {
        if(getLogger().isDebugging()) {
          getLogger().debug("reinsert " + entry);
        }
        insertLeafEntry(entry);
      }
      else {
        if(getLogger().isDebugging()) {
          getLogger().debug("reinsert " + entry + " at " + level);
        }
        insertDirectoryEntry(entry, level);
      }
    }
  }

  /**
   * Adjusts the tree after insertion of some nodes.
   * 
   * @param subtree the subtree to be adjusted
   */
  protected void adjustTree(IndexTreePath<E> subtree) {
    if(getLogger().isDebugging()) {
      getLogger().debugFine("Adjust tree " + subtree);
    }

    // get the root of the subtree
    N node = getNode(subtree.getLastPathComponent().getEntry());

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
          N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
          if(getLogger().isDebugging()) {
            getLogger().debugFine("parent " + parent);
          }
          parent.addDirectoryEntry(createNewDirectoryEntry(split));

          // adjust the entry representing the (old) node, that has
          // been split

          // This does not work in the persistent version
          // node.adjustEntry(subtree.getLastPathComponent().getEntry());
          node.adjustEntry(parent.getEntry(subtree.getLastPathComponent().getIndex()));

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
        N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
        E entry = parent.getEntry(subtree.getLastPathComponent().getIndex());
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
    N node = getNode(subtree.getLastPathComponent().getEntry());
    // node is not root
    if(!isRoot(node)) {
      N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
      int index = subtree.getLastPathComponent().getIndex();
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
      if(hasUnderflow(node) & node.getNumEntries() == 1 && !node.isLeaf()) {
        N child = getNode(node.getEntry(0));
        N newRoot;
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
    List<E> result = new ArrayList<E>();

    if(height == 1) {
      result.add(getRootEntry());
      return result;
    }

    getLeafNodes(getRoot(), result, height);
    return result;
  }

  /**
   * Determines the entries pointing to the leaf nodes of the specified subtree
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
        N child = getNode(node.getEntry(i));
        getLeafNodes(child, result, (currentLevel - 1));
      }
    }
  }

  /**
   * Perform additional integrity checks.
   */
  public void doExtraIntegrityChecks() {
    if(extraIntegrityChecks) {
      getRoot().integrityCheck(this);
    }
  }

  /**
   * Returns a string representation of this R*-Tree.
   * 
   * @return a string representation of this R*-Tree
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    if(initialized) {
      N node = getRoot();
      int dim = node.getDimensionality();

      while(!node.isLeaf()) {
        if(node.getNumEntries() > 0) {
          E entry = node.getEntry(0);
          node = getNode(entry);
          levels++;
        }
      }

      BreadthFirstEnumeration<N, E> enumeration = new BreadthFirstEnumeration<N, E>(this, getRootPath());
      while(enumeration.hasMoreElements()) {
        IndexTreePath<E> indexPath = enumeration.nextElement();
        E entry = indexPath.getLastPathComponent().getEntry();
        if(entry.isLeafEntry()) {
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
      result.append(getClass().getName()).append(" has ").append((levels + 1)).append(" levels.\n");
      result.append(dirNodes).append(" Directory Knoten (max = ").append(dirCapacity - 1).append(", min = ").append(dirMinimum).append(")\n");
      result.append(leafNodes).append(" Daten Knoten (max = ").append(leafCapacity - 1).append(", min = ").append(leafMinimum).append(")\n");
      result.append(objects).append(" ").append(dim).append("-dim. Punkte im Baum \n");
      PageFileUtil.appendPageFileStatistics(result, getPageFileStatistics());
    }
    else {
      result.append(getClass().getName()).append(" is empty!\n");
    }

    return result.toString();
  }
}