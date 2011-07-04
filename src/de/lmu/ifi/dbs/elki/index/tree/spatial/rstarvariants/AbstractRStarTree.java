package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialComparator;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.Enlargement;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;

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
 * @apiviz.composedOf TopologicalSplit
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
   * Contains a boolean for each level of this R*-Tree that indicates if there
   * was already a reinsert operation in this level during the current insert /
   * delete operation.
   */
  protected final Map<Integer, Boolean> reinsertions = new HashMap<Integer, Boolean>();

  /**
   * The height of this R*-Tree.
   */
  protected int height;

  /**
   * For counting the number of distance computations.
   */
  public int distanceCalcs = 0;

  /**
   * Defines how many children are tested for finding the child generating the
   * least overlap when inserting an object. Default 0 means all children
   */
  int insertionCandidates = 0;

  /**
   * The last inserted entry
   */
  E lastInsertedEntry = null;

  /**
   * Constructor
   * 
   * @param pagefile Page file
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   * @param insertionCandidates insertion candidate set size
   */
  public AbstractRStarTree(PageFile<N> pagefile, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(pagefile, bulk, bulkLoadStrategy);
    this.insertionCandidates = insertionCandidates;
  }

  /**
   * Test whether a bulk insert is still possible.
   * 
   * @return Success code
   */
  public boolean canBulkLoad() {
    return (bulk && !initialized);
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
        if(((LeafEntry) node.getEntry(i)).getDBID() == id) {
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
    reinsertions.clear();

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
      getLogger().debugFine("insertion-subtree " + subtree + "\n");
    }

    N parent = getNode(subtree.getLastPathComponent().getEntry());
    parent.addLeafEntry(entry);
    file.writePage(parent);

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
    file.writePage(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);
  }
  
  /**
   * Delete a leaf at a given path - deletions for non-leaves are not supported!
   * 
   * @param deletionPath Path to delete
   */
  public void deletePath(IndexTreePath<E> deletionPath) {
    N leaf = getNode(deletionPath.getParentPath().getLastPathComponent().getEntry());
    int index = deletionPath.getLastPathComponent().getIndex();

    // delete o
    E entry = leaf.getEntry(index);
    leaf.deleteEntry(index);
    file.writePage(leaf);

    // condense the tree
    Stack<N> stack = new Stack<N>();
    condenseTree(deletionPath.getParentPath(), stack);

    // reinsert underflow nodes
    while(!stack.empty()) {
      N node = stack.pop();
      if(node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          reinsertions.clear();
          this.insertLeafEntry(node.getEntry(i));
        }
      }
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          stack.push(getNode(node.getEntry(i)));
        }
      }
      file.deletePage(node.getPageID());
    }
    postDelete(entry);

    doExtraIntegrityChecks();
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
   * Returns the height of this R*-Tree.
   * 
   * @return the height of this R*-Tree
   */
  public final int getHeight() {
    return height;
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

    if(file != null && initialized) {
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
      PageFileUtil.appendPageFileStatistics(result, file);
    }
    else {
      result.append(getClass().getName()).append(" is empty!\n");
    }

    return result.toString();
  }

  /**
   * Initializes this R*-Tree from an existing persistent file.
   */
  @Override
  public void initializeFromFile(TreeIndexHeader header) {
    super.initializeFromFile(header);
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
      while(baos.size() <= file.getPageSize()) {
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
      HyperBoundingBox hb = new HyperBoundingBox(new double[exampleLeaf.getDimensionality()], new double[exampleLeaf.getDimensionality()]);
      SpatialDirectoryEntry sl = new SpatialDirectoryEntry(0, hb);
      while(baos.size() <= file.getPageSize()) {
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
      throw new IllegalArgumentException("Node size of " + file.getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      getLogger().warning("Page size is choosen very small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.4);
    if(dirMinimum < 2) {
      dirMinimum = 2;
    }

    if(leafCapacity <= 1) {
      throw new IllegalArgumentException("Node size of " + file.getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      getLogger().warning("Page size is choosen very small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.4);
    if(leafMinimum < 2) {
      leafMinimum = 2;
    }

    if(getLogger().isVerbose()) {
      getLogger().verbose("Directory Capacity:  " + (dirCapacity - 1) + "\nDirectory minimum: " + dirMinimum + "\nLeaf Capacity:     " + (leafCapacity - 1) + "\nLeaf Minimum:      " + leafMinimum);
    }
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
    BulkSplit<E> split = new BulkSplit<E>();
    List<List<E>> partitions = split.partition(objects, minEntries, maxEntries, bulkLoadStrategy);

    for(List<E> partition : partitions) {
      // create leaf node
      N leafNode = createNewLeafNode(leafCapacity);
      file.writePage(leafNode);
      result.add(leafNode);

      // insert data
      for(E o : partition) {
        leafNode.addLeafEntry(o);
      }

      // write to file
      file.writePage(leafNode);

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
   * Sets the height of this R*-Tree.
   * 
   * @param height the height to be set
   */
  protected void setHeight(int height) {
    this.height = height;
  }

  /**
   * Clears the reinsertions.
   */
  protected void clearReinsertions() {
    reinsertions.clear();
  }

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
   * Computes the height of this RTree. Is called by the constructor.
   * 
   * @return the height of this RTree
   */
  abstract protected int computeHeight();

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor.
   */
  abstract protected void bulkLoad(List<E> entrys);

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   * @return the newly created directory entry
   */
  abstract protected E createNewDirectoryEntry(N node);

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
    if (node == null) {
      throw new RuntimeException("Page file did not return node for node id: "+subtree.getLastPathComponent().getEntry().getEntryID());
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
    // children are leafs
    if(childNode.isLeaf()) {
      if(height - subtree.getPathCount() == level) {
        TreeIndexPathComponent<E> comp = null;
        if(insertionCandidates == 0) {
          comp = getChildWithLeastOverlap(node, mbr);
        }
        else {
          comp = getChildWithLeastOverlapFast(node, mbr);
        }
        return subtree.pathByAddingChild(comp);
      }
      else {
        throw new IllegalArgumentException("childNode is leaf, but currentLevel != level: " + (height - subtree.getPathCount()) + " != " + level);
      }
    }
    // children are directory nodes
    else {
      IndexTreePath<E> newSubtree = subtree.pathByAddingChild(getLeastEnlargement(node, mbr));
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
   * Returns the path information of the entry of the specified node with the
   * least enlargement if the given mbr would be inserted into.
   * 
   * @param node the node which children have to be tested
   * @param mbr the mbr of the node to be inserted
   * @return the path information of the entry with the least enlargement if the
   *         given mbr would be inserted into
   */
  private TreeIndexPathComponent<E> getLeastEnlargement(N node, SpatialComparable mbr) {
    Enlargement<E> min = null;

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      double volume = SpatialUtil.volume(entry);
      HyperBoundingBox newMBR = SpatialUtil.union(entry, mbr);
      double inc = SpatialUtil.volume(newMBR) - volume;
      Enlargement<E> enlargement = new Enlargement<E>(new TreeIndexPathComponent<E>(entry, i), volume, inc, 0);

      if(min == null || min.compareTo(enlargement) > 0) {
        min = enlargement;
      }
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Returns the path information of the entry of the specified node which needs
   * least overlap enlargement if the given mbr would be inserted into.
   * 
   * @param node the node of which the children should be tested
   * @param mbr the mbr to be inserted into the children
   * @return the path information of the entry which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  protected TreeIndexPathComponent<E> getChildWithLeastOverlap(N node, SpatialComparable mbr) {
    Enlargement<E> min = null;

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry_i = node.getEntry(i);
      HyperBoundingBox newMBR = union(mbr, entry_i);

      double currOverlap = 0;
      double newOverlap = 0;
      for(int k = 0; k < node.getNumEntries(); k++) {
        if(i != k) {
          E entry_k = node.getEntry(k);
          currOverlap += SpatialUtil.relativeOverlap(entry_i, entry_k);
          newOverlap += SpatialUtil.relativeOverlap(newMBR, entry_k);
        }
      }

      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement<E> enlargement = new Enlargement<E>(new TreeIndexPathComponent<E>(entry_i, i), volume, inc_volume, inc_overlap);

      if(min == null || min.compareTo(enlargement) > 0) {
        min = enlargement;
      }
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Returns the path information of the entry of the specified node which needs
   * least overlap enlargement if the given mbr would be inserted into.
   * 
   * @param node the node of which the children should be tested
   * @param mbr the mbr to be inserted into the children
   * @return the path information of the entry which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  protected TreeIndexPathComponent<E> getChildWithLeastOverlapFast(N node, SpatialComparable mbr) {
    Enlargement<E> min = null;

    TopBoundedHeap<FCPair<Double, E>> entriesToTest = new TopBoundedHeap<FCPair<Double, E>>(insertionCandidates, Collections.reverseOrder());
    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry_i = node.getEntry(i);
      HyperBoundingBox newMBR = union(mbr, entry_i);
      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      entriesToTest.add(new FCPair<Double, E>(inc_volume, entry_i));
    }

    while(!entriesToTest.isEmpty()) {
      E entry_i = entriesToTest.poll().getSecond();
      int index = -1;
      HyperBoundingBox newMBR = union(mbr, entry_i);

      double currOverlap = 0;
      double newOverlap = 0;
      for(int k = 0; k < node.getNumEntries(); k++) {
        E entry_k = node.getEntry(k);
        if(entry_i != entry_k) {
          currOverlap += SpatialUtil.relativeOverlap(entry_i, entry_k);
          newOverlap += SpatialUtil.relativeOverlap(newMBR, entry_k);
        }
        else {
          index = k;
        }
      }

      double volume = /* entry_i.getMBR() == null ? 0 : */SpatialUtil.volume(entry_i);
      double inc_volume = SpatialUtil.volume(newMBR) - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement<E> enlargement = new Enlargement<E>(new TreeIndexPathComponent<E>(entry_i, index), volume, inc_volume, inc_overlap);

      if(min == null || min.compareTo(enlargement) > 0) {
        min = enlargement;
      }
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Returns the union of the two specified MBRs.
   * 
   * @param mbr1 the first MBR
   * @param mbr2 the second MBR
   * @return the union of the two specified MBRs
   */
  protected HyperBoundingBox union(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1 == null && mbr2 == null) {
      return null;
    }
    if(mbr1 == null) {
      // Clone - intentionally
      return new HyperBoundingBox(mbr2);
    }
    if(mbr2 == null) {
      // Clone - intentionally
      return new HyperBoundingBox(mbr1);
    }
    return SpatialUtil.union(mbr1, mbr2);
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
    int level = height - path.getPathCount() + 1;
    Boolean reInsert = reinsertions.get(level);

    // there was still no reinsert operation at this level
    if(node.getPageID() != 0 && (reInsert == null || !reInsert)) {
      reinsertions.put(level, true);
      if(getLogger().isDebugging()) {
        getLogger().debugFine("REINSERT " + reinsertions + "\n");
      }
      reInsert(node, level, path);
      return null;
    }

    // there was already a reinsert operation at this level
    else {
      return split(node);
    }
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
    TopologicalSplit<E> split = new TopologicalSplit<E>(node.getEntries(), minimum);

    // do the split
    N newNode;
    if(split.getBestSorting() == SpatialComparator.MIN) {
      newNode = node.splitEntries(split.getMinSorting(), split.getSplitPoint());
    }
    else if(split.getBestSorting() == SpatialComparator.MAX) {
      newNode = node.splitEntries(split.getMaxSorting(), split.getSplitPoint());
    }
    else {
      throw new IllegalStateException("split.bestSort is undefined: " + split.getBestSorting());
    }

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    if(getLogger().isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("Split Node ").append(node.getPageID()).append(" (").append(getClass()).append(")\n");
      msg.append("      splitAxis ").append(split.getSplitAxis()).append("\n");
      msg.append("      splitPoint ").append(split.getSplitPoint()).append("\n");
      msg.append("      newNode ").append(newNode.getPageID()).append("\n");
      getLogger().debugFine(msg.toString());
    }

    return newNode;
  }

  /**
   * Reinserts the specified node at the specified level.
   * 
   * @param node the node to be reinserted
   * @param level the level of the node
   * @param path the path to the node
   */
  @SuppressWarnings("unchecked")
  protected void reInsert(N node, int level, IndexTreePath<E> path) {
    EuclideanDistanceFunction distFunction = EuclideanDistanceFunction.STATIC;
    DistanceEntry<DoubleDistance, E>[] reInsertEntries = new DistanceEntry[node.getNumEntries()];

    // compute the center distances of entries to the node and sort it
    // in decreasing order to their distances
    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      DoubleDistance dist = distFunction.centerDistance(node, entry);
      reInsertEntries[i] = new DistanceEntry<DoubleDistance, E>(entry, dist, i);
    }
    Arrays.sort(reInsertEntries, Collections.reverseOrder());

    // define, how many entries will be reinserted
    int start = (int) (0.3 * node.getNumEntries());

    // initialize the reinsertion operation: move the remaining entries
    // forward
    node.initReInsert(start, reInsertEntries);
    file.writePage(node);

    // and adapt the mbrs
    IndexTreePath<E> childPath = path;
    N child = node;
    while(childPath.getParentPath() != null) {
      N parent = getNode(childPath.getParentPath().getLastPathComponent().getEntry());
      int indexOfChild = childPath.getLastPathComponent().getIndex();
      child.adjustEntry(parent.getEntry(indexOfChild));
      file.writePage(parent);
      childPath = childPath.getParentPath();
      child = parent;
    }

    // reinsert the first entries
    for(int i = 0; i < start; i++) {
      DistanceEntry<DoubleDistance, E> re = reInsertEntries[i];
      if(node.isLeaf()) {
        if(getLogger().isDebugging()) {
          getLogger().debugFine("reinsert " + re.getEntry());
        }
        insertLeafEntry(re.getEntry());
      }
      else {
        if(getLogger().isDebugging()) {
          getLogger().debugFine("reinsert " + re.getEntry() + " at " + level);
        }
        insertDirectoryEntry(re.getEntry(), level);
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
      getLogger().debugFine("Adjust tree " + subtree + "\n");
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
        if(node.getPageID().equals(getRootEntry().getEntryID())) {
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
          file.writePage(parent);
          adjustTree(subtree.getParentPath());
        }
      }
    }
    // no overflow, only adjust parameters of the entry representing the
    // node
    else {
      if(!node.getPageID().equals(getRootEntry().getEntryID())) {
        N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
        int index = subtree.getLastPathComponent().getIndex();
        lastInsertedEntry = node.adjustEntryIncremental(parent.getEntry(index), lastInsertedEntry);
        // node.adjustEntry(parent.getEntry(index));
        // write changes in parent to file
        file.writePage(parent);
        adjustTree(subtree.getParentPath());
      }
      // root level is reached
      else {
        node.adjustEntry(getRootEntry());
      }
    }
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified object.
   * 
   * @param node the node
   * @param q the query object
   * @param distanceFunction the distance function for computing the distances
   * @return a list of the sorted entries
   */
  // TODO: move somewhere else?
  protected <D extends Distance<D>> List<DistanceEntry<D, E>> getSortedEntries(AbstractRStarTreeNode<?, ?> node, SpatialComparable q, SpatialPrimitiveDistanceFunction<?, D> distanceFunction) {
    List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      @SuppressWarnings("unchecked")
      E entry = (E) node.getEntry(i);
      D minDist = distanceFunction.minDist(entry, q);
      result.add(new DistanceEntry<D, E>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
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
    if(!node.getPageID().equals(getRootEntry().getEntryID())) {
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
      file.writePage(parent);
      // get subtree to parent
      condenseTree(subtree.getParentPath(), stack);
    }

    // node is root
    else {
      if(hasUnderflow(node) & node.getNumEntries() == 1 && !node.isLeaf()) {
        N child = getNode(node.getEntry(0));
        N newRoot;
        if(child.isLeaf()) {
          newRoot = createNewLeafNode(leafCapacity);
          newRoot.setPageID(getRootEntry().getEntryID());
          for(int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addLeafEntry(child.getEntry(i));
          }
        }
        else {
          newRoot = createNewDirectoryNode(dirCapacity);
          newRoot.setPageID(getRootEntry().getEntryID());
          for(int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addDirectoryEntry(child.getEntry(i));
          }
        }
        file.writePage(newRoot);
        height--;
      }
    }
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
        N child = file.readPage(node.getEntry(i).getEntryID());
        getLeafNodes(child, result, (currentLevel - 1));
      }
    }
  }

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
    N root = createNewDirectoryNode(dirCapacity);
    file.writePage(root);

    // switch the ids
    oldRoot.setPageID(root.getPageID());
    if(!oldRoot.isLeaf()) {
      for(int i = 0; i < oldRoot.getNumEntries(); i++) {
        N node = getNode(oldRoot.getEntry(i));
        file.writePage(node);
      }
    }

    root.setPageID(getRootEntry().getEntryID());
    E oldRootEntry = createNewDirectoryEntry(oldRoot);
    E newNodeEntry = createNewDirectoryEntry(newNode);
    root.addDirectoryEntry(oldRootEntry);
    root.addDirectoryEntry(newNodeEntry);

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
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
   * Perform additional integrity checks.
   */
  public void doExtraIntegrityChecks() {
    if(extraIntegrityChecks) {
      getRoot().integrityCheck(this.file);
    }
  }
}