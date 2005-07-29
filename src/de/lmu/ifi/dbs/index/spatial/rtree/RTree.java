package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RTree is a spatial index structure. Apart from organizing the objects
 * it also provides several methods to search for certain object in the
 * structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class RTree implements SpatialIndex {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level loggerLevel = Level.OFF;

  /**
   * The file storing the entries of this RTree.
   */
  private PageFile file;

  /**
   * The height of the current insert operation.
   * TODO sinn?????
   */
  private int height;

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be indexed
   * @param fileName       the name of the file for storing the entries,
   *                       if this parameter is null all entries will be hold in
   *                       main memory
   * @param pageSize       the size of a page in bytes
   * @param cacheSize      the size of the cache (must be >= 1)
   * @param flatDirectory  id true, this RTree will have a flat directory
   *                       (only one loggerLevel)
   */
  public RTree(int dimensionality, String fileName, int pageSize,
               int cacheSize, boolean flatDirectory) {
    initLogger();

    if (fileName == null) {
      this.file = new MemoryPageFile(cacheSize, PageFile.LRU_CACHE);
    }
    else {
      this.file = new PersistentPageFile(cacheSize, PageFile.LRU_CACHE, fileName);
    }
    this.file.initialize(dimensionality, pageSize, flatDirectory);

    Node rootNode = new LeafNode(file);
    file.writeNode(rootNode);

    String msg = "Capacity " + (file.getCapacity() - 1) + "\n" +
      "root: " + rootNode.getPageID() + "\n";

    logger.info(msg);
  }

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param objects       the data objects to be indexed
   * @param fileName      the name of the file for storing the entries,
   *                      if this parameter is null all entries will be hold in
   *                      main memory
   * @param pageSize      the size of a page in bytes
   * @param cacheSize     the size of the cache (must be >= 1)
   * @param flatDirectory id true, this RTree will have a flat directory
   *                      (only one loggerLevel)
   */
  public RTree(SpatialData[] objects, String fileName, int pageSize,
               int cacheSize, boolean flatDirectory) {
    initLogger();

    int dimension = objects[0].getValues().length;

    if (fileName == null) {
      this.file = new MemoryPageFile(cacheSize, PageFile.LRU_CACHE);
    }
    else {
      this.file = new PersistentPageFile(cacheSize, PageFile.LRU_CACHE, fileName);
    }
    this.file.initialize(dimension, pageSize, flatDirectory);

    StringBuffer msg = new StringBuffer();
    int maxLoad = file.getMaximum();
    msg.append("\n  maxLoad = " + maxLoad);

    // root is leaf node
    if ((double) objects.length / (double) maxLoad <= 1) {
      LeafNode root = new LeafNode(file);
      file.writeNode(root);
      createRoot(root, objects);
      msg.append("\n  numNodes = 1");
    }

    // root is directory node
    else {
      DirectoryNode root = new DirectoryNode(file);
      file.writeNode(root);

      // create leaf nodes
      Node[] nodes = createLeafNodes(objects);
      int numNodes = nodes.length;
      msg.append("\n  numLeafNodes = " + numNodes);

      // create directory nodes
      while (nodes.length > maxLoad) {
        nodes = createDirectoryNodes(nodes);
        numNodes += nodes.length;
      }
      // create root
      createRoot(root, nodes);
      numNodes++;
      msg.append("\n  numNodes = " + numNodes);
    }
    logger.info(msg.toString() + "\n");
  }

  /**
   * Inserts the specified object into this index.
   *
   * @param o the data object to be inserted
   */
  public synchronized void insert(SpatialData o) {
    insert(o, new TreeMap<Integer, Boolean>());
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   *         TODO test rausnehmen!
   */

  public synchronized boolean delete(SpatialData o) {
    /*logger.info("delete " + o + "\n");
    Deletion del = findLeaf(file.readNode(0), o);

    if (del == null)
      return false;

    Node leaf = del.leaf;
    int index = del.index;

    leaf.deleteEntry(index);
    file.writeNode(leaf);

    Stack stack = new Stack();
    condenseTree(leaf, stack);

    while (!stack.empty()) {
      Node node = (Node) stack.pop();
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          Entry e = node.entries[i];
          Data obj = new Data(e.getID(), e.getMBR().getMinClone(), -1);
          this.insert(obj);
        }
      }
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          stack.push(file.readNode(node.entries[i].getID()));
        }
      }
      file.deleteNode(node.getPageID());
    }

    try {
      Node root = file.readNode(0);
      root.test();
    }
    catch (RuntimeException e) {
      throw e;
    }
    */

    return true;
  }

  /**
   * Performs a range query for the given spatial objec with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  /*
  public List<QueryResult> rangeQuery(SpatialData obj, String epsilon, SpatialDistanceFunction distanceFunction) {
    final List<QueryResult> result = new ArrayList<QueryResult>();
    final Heap pq = new Heap();

    // push root
    pq.addNode(new DefaultHeapNode(getRoot(), distanceFunction.nullDistance()));

    // search in tree
    while (!pq.isEmpty()) {
      DefaultHeapNode pqNode = (DefaultHeapNode) pq.getMinNode();
      if (pqNode.getKey().compareTo(range) > 0) break;

      Node node = (Node) pqNode.getObject();
      final int numEntries = node.getNumEntries();

      for (int i = 0; i < numEntries; i++) {
        Distance distance = distanceFunction.minDist(node.getEntry(i).getMBR(), o);
        if (distance.compareTo(range) <= 0) {
          Entry entry = node.getEntry(i);
          if (node.isLeaf()) {
            DBObject neighbor = new DBObject(entry.getID(), entry.getMBR().getMin(), node.getPageID());
            result.add(new DBNeighbor(neighbor, distance));
          }
          else {
            Node childNode = file.readNode(entry.getID());
            pq.addNode(new DefaultHeapNode(childNode, distance));
          }
        }
      }
    }

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  public List<DBNeighbor> kNNQuery(Indexable o, int k, SpatialDistanceFunction distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    // variables
    final Heap pq = new Heap();
    final KNNList knnList = new KNNList(k, distanceFunction);

    // push root
    pq.addNode(new DefaultHeapNode(file.readNode(0), distanceFunction.nullDistance()));
    Distance maxDist = distanceFunction.infiniteDistance();
    // search in tree

    while (!pq.isEmpty()) {
      DefaultHeapNode pqNode = (DefaultHeapNode) pq.getMinNode();

      if (pqNode.getKey().compareTo(maxDist) > 0) {
        return knnList.toList();
      }

      Node node = (Node) pqNode.getObject();
      int numEntries = node.getNumEntries();
      // data node
      if (node.isLeaf()) {
        for (int i = 0; i < numEntries; i++) {
          Entry entry = node.getEntry(i);
          Distance distance = distanceFunction.minDist(entry.getMBR(), o);
          if (distance.compareTo(maxDist) <= 0) {
            DBObject obj = new DBObject(entry.getID(), entry.getMBR().getMin(), node.getPageID());
            knnList.add(new DBNeighbor(obj, distance));
            if (knnList.size() == k) {
              maxDist = knnList.getMaximumDistance();
            }
          }
        }
      }
      // directory node
      else {
        for (int i = 0; i < numEntries; i++) {
          Entry entry = node.getEntry(i);
          Distance distance = distanceFunction.minDist(entry.getMBR(), o);
          if (distance.compareTo(maxDist) <= 0) {
            pq.addNode(new DefaultHeapNode(file.readNode(entry.getID()), distance));
          }

        }
      }
    }

    return knnList.toList();

  }

  public IndexableIterator dataIterator() {
    return new IndexableIterator() {
      BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());

      public boolean hasNext() {
        // last element must be a data enumeration
        return enumeration.hasMoreElements();
      }

      public Indexable next() {
        Object o = enumeration.nextElement();
        while (o instanceof Node && enumeration.hasMoreElements()) {
          o = enumeration.nextElement();
        }
        return (Indexable) o;
      }
    };
  }

  /**
   * Returns the I/O-access of this RTree.
   * @return the I/O-access of this RTree
   */
  public int getIOAccess() {
    return file.getIOAccess();
  }

  public void resetIOAccess() {
    file.setIOAccess(0);
  }

  /*public LeafIterator leafIterator() {
    throw new UnsupportedOperationException();
  } */

  public SpatialNode getNode(int pageID) {
    return file.readNode(pageID);
  }

  public SpatialNode getRoot() {
    return file.readNode(0);
  }

  /*
  public int test() {
    int io = file.getIOAccess();

    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;
    final Vector<SpatialNode> v = new Vector<SpatialNode>();
    v.add(getRoot());

    Node node = (Node) getRoot();
    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = file.readNode(entry.getID());
        levels++;
      }
    }

    BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());
    while (enumeration.hasMoreElements()) {
      SpatialEntry entry = enumeration.nextElement();
      if (entry instanceof Indexable)
        objects++;
      else {
        node = (Node) entry;
        if (node.isLeaf())
          leafNodes++;
        else
          dirNodes++;
      }
    }

    file.setIOAccess(io);

    result.append("RTree hat " + (levels + 1) + " Ebenen \n");
    result.append(dirNodes + " Directory Knoten \n");
    result.append(leafNodes + " Daten Knoten (capacity = " + (file.getMaximum()) + ")\n");
    result.append(objects + " " + file.getDimensionality() + "-dim. Punkte im Baum \n");

    return objects;
  }

 /**
  * Returns a string representation of this RTree.
  * @return a string representation of this RTree
  */
  /*
  public String toString() {
    int io = file.getIOAccess();

    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;
    final Vector<SpatialNode> v = new Vector<SpatialNode>();
    v.add(getRoot());

    Node node = (Node) getRoot();
    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = file.readNode(entry.getID());
        levels++;
      }
    }

    BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());
    while (enumeration.hasMoreElements()) {
      SpatialEntry entry = enumeration.nextElement();
      if (entry instanceof Indexable)
        objects++;
      else {
        node = (Node) entry;
        if (node.isLeaf())
          leafNodes++;
        else
          dirNodes++;
      }
    }

    file.setIOAccess(io);

    result.append("RTree hat " + (levels + 1) + " Ebenen \n");
    result.append(dirNodes + " Directory Knoten \n");
    result.append(leafNodes + " Daten Knoten (max = " + (file.getMaximum()) + ", min = " + file.getMinimum() + ")\n");
    result.append(objects + " " + file.getDimensionality() + "-dim. Punkte im Baum \n");
    return result.toString();
  }

  /**
   * Creates and returns a new root node that points to the two specified child nodes.
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return a new root node that points to the two specified child nodes
   */
  private DirectoryNode createNewRoot(final Node oldRoot, final Node newNode) {
    logger.info("create new root");
    DirectoryNode root = new DirectoryNode(file);
    file.writeNode(root);

    oldRoot.pageID = root.getPageID();
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        Node node = file.readNode(oldRoot.entries[i].getID());
        node.parentID = oldRoot.pageID;
        file.writeNode(node);
      }
    }

    root.pageID = 0;
    root.addEntry(oldRoot);
    root.addEntry(newNode);

    file.writeNode(root);
    file.writeNode(oldRoot);
    file.writeNode(newNode);
    String msg = "New Root-ID " + root.pageID + "\n";
    logger.info(msg);

    return root;
  }

  /**
   *
   * @param node
   * @param mbr
   * @param currentLevel
   * @param level
   * @return
   */
  private Node chooseSubtree(Node node, MBR mbr, int currentLevel, int level) {
    logger.info("node " + node + ", currentLevel " + currentLevel + ", height " + level);

    Entry entry = node.entries[0];
    Node childNode = file.readNode(entry.getID());
    // children are leafs
    if (childNode.isLeaf()) {
      if (currentLevel == level)
        return getLeastOverlap(node, mbr);
      else
        return chooseSubtree(getLeastOverlap(node, mbr), mbr, currentLevel, level - 1);
    }
    // children are directory nodes
    else {
      if (currentLevel == level)
        return getLeastEnlargement(node, mbr);
      else
        return chooseSubtree(getLeastEnlargement(node, mbr), mbr, currentLevel, level - 1);
    }
  }

  private LeafNode chooseLeaf(Node node, MBR mbr) {
    height++;
    if (node.isLeaf()) return (LeafNode) node;

    Entry entry = node.entries[0];
    Node childNode = file.readNode(entry.getID());
    // children are leafs
    if (childNode.isLeaf()) {
      return chooseLeaf(getLeastOverlap(node, mbr), mbr);
    }
    // children are directory nodes
    else {
      return chooseLeaf(getLeastEnlargement(node, mbr), mbr);
    }
  }

  private Node getLeastEnlargement(Node node, MBR mbr) {
    Enlargement min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry = node.entries[i];
      double volume = entry.getMBR().volume();
      MBR newMBR = entry.getMBR().union(mbr);
      double inc = newMBR.volume() - volume;
      Enlargement enlargement = new Enlargement(entry.getID(), volume, inc, 0);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    return file.readNode(min.pageID);
  }

  private Node getLeastOverlap(Node node, MBR mbr) {
    Enlargement min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry_i = node.entries[i];
      MBR newMBR = entry_i.getMBR().union(mbr);

      double currOverlap = 0;
      double newOverlap = 0;
      for (int k = 0; k < node.getNumEntries(); k++) {
        if (i != k) {
          Entry entry_k = node.entries[k];
          currOverlap += entry_i.getMBR().overlap(entry_k.getMBR());
          newOverlap += newMBR.overlap(entry_k.getMBR());
        }
      }

      double volume = entry_i.getMBR().volume();
      double inc_volume = newMBR.volume() - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement enlargement = new Enlargement(entry_i.getID(), volume, inc_volume, inc_overlap);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    return file.readNode(min.pageID);
  }

  /*
  protected Node overflowTreatment(Node node, int level, final int height, Map<Integer, Boolean> reInsert) {
    Boolean reIns = reInsert.get(new Integer(level));
    // there was still no reinsert operation at this loggerLevel
    if (node.getPageID() != 0 && (reIns == null || !reIns.booleanValue())) {
      reInsert.put(new Integer(level), new Boolean(true));
      reInsert(node, level, height, reInsert);
      return null;
    }
    // there was already an reinsert operation at this loggerLevel
    else {
      Node split = split(node);
      return split;
    }
  }

  private Node split(Node node) {
    SplitDescription split = new SplitDescription();
    // choose the split dimension
    split.chooseSplitAxis(node.entries, file.getMinimum());
    // choose the split point
    split.chooseSplitPoint(node.entries, file.getMinimum());

    // do the split
    Node newNode = null;

    if (split.bestSort == SpatialComparator.MIN) {
      newNode = node.splitEntries(split.minSorting, split.splitPoint);
    }
    else {
      newNode = node.splitEntries(split.maxSorting, split.splitPoint);
    }

    String msg = "Split Node " + node.getPageID() + " (" + this.getClass() + ")\n" +
      "      splitAxis " + split.splitAxis + "\n" +
      "      splitPoint " + split.splitPoint + "\n" +
      "      newNode " + newNode.getPageID() + "\n";

    logger.info(msg);

    file.writeNode(node);
    file.writeNode(newNode);

    return newNode;
  }

  /*
  private void reInsert(Node node, int level, final int height, Map<Integer, Boolean> reInsert) {
    MBR mbr = node.mbr();
    SpatialDistanceFunction distFunction = new EuklidDistanceFunction();
    ReinsertEntry[] reInsertEntries = new ReinsertEntry[node.getNumEntries()];

    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry = node.entries[i];
      Distance dist = distFunction.centerDistance(mbr, entry.getMBR());
      reInsertEntries[i] = new ReinsertEntry(entry.getID(), entry.getMBR(), dist);
    }
    Arrays.sort(reInsertEntries);

    int start = (int) (0.3 * (double) node.getNumEntries());

    // löschen und nach vorne schieben
    node.initReInsert(start, reInsertEntries);
    file.writeNode(node);
    // und  mbrs in parent anpassen
    Node child = node;
    while (child.parentID != -1) {
      Node parent = file.readNode(child.parentID);
      parent.entries[child.index].setMBR(child.mbr());
      file.writeNode(parent);
      child = parent;
    }

    for (int i = 0; i < start; i++) {
      ReinsertEntry re = reInsertEntries[i];
      if (node.isLeaf()) {
        Data o = new Data(re.getID(), re.getMBR().getMinClone(), -1);
        insert(o, reInsert);
      }
      else {
        Node reNode = file.readNode(re.getID());
        insert(reNode, level, height, reInsert);
      }
    }
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  private boolean hasOverflow(Node node) {
    if (node.isLeaf())
      return node.getNumEntries() == file.getCapacity();
    else
      return ! file.isFlatDirectory() && node.getNumEntries() == file.getCapacity();
  }

  /**
   * Inserts the specified data object into this RTree.
   *
   * @param o        the object to be inserted
   * @param reInsert a map containing a boolean for each loggerLevel that indicates
   *                 if there was already a reinsert operation in this loggerLevel
   *                 during the current insert / delete operation
   *                 TODO test abfragen raus
   */
  private synchronized void insert(SpatialData o, Map<Integer, Boolean> reInsert) {
    /*
    logger.info("insert " + o + "\n");

    MBR mbr = new MBR(o.getValues(), o.getValues());
    height = -1;

    // choose leaf for o and insert o into leaf
    Node node = chooseLeaf(file.readNode(0), mbr);
    node.addEntry(o);
    file.writeNode(node);

    // adjust tree
    int currentLevel = 0;
    while (node != null) {
      node = file.readNode(node.getPageID());

      // todo test raus
      Node fnode = file.readNode(node.getPageID());
      if (!node.equals(fnode)) {
//        System.out.println("node "+node + " != fnode " + fnode);
//        System.out.println("node.parent "+node.getParent() + " != fnode.parent " + fnode.getParent());
//        System.out.println("node.place "+node.getPlace() + " != fnode.place " + fnode.getPlace());
//        System.out.println("node.entries "+ Arrays.asList(node.getEntries()) + " !=  " +
//                           "fnode.entries " + Arrays.asList(fnode.getEntries()));
        throw new RuntimeException("!node.equals(fnode)");
      }

      // hasOverflow occured
      if (hasOverflow(node)) {
        Node split = overflowTreatment(node, currentLevel++, height, reInsert);
        if (split != null) {
          if (node.getPageID() != 0) {
            Node parent = file.readNode(node.parentID);
            parent.addEntry(split);

            Entry entry1 = parent.entries[node.index];
            MBR mbr1 = node.mbr();
            entry1.setMBR(mbr1);
            Entry entry2 = parent.entries[split.index];
            MBR mbr2 = split.mbr();
            entry2.setMBR(mbr2);
            file.writeNode(parent);
            node = parent;
          }
          // if root was split: create a new root that points the two split nodes
          else {
            node = createNewRoot(node, split);
          }
        }
        else {
          currentLevel--;
        }
      }
      // no overflow, only adjust mbrs in parent
      else if (node.getPageID() != 0) {
        fnode = file.readNode(node.getPageID());
        if (!fnode.equals(node)) throw new RuntimeException();


        Node parent = file.readNode(node.parentID);
        if (parent.entries[node.index].getID() != node.getPageID()) {
          throw new RuntimeException();
        }

//        for (int i=0; i<parent.getNumEntries(); i++) {
//          Entry e = parent.getEntry(i);
//          System.out.println(e + " " + e.getClass() + " , mbr "+e.getMBR());
//        }

        Entry entry = parent.entries[node.index];

//        System.out.println("NODE "+node + " ("+node.getPlace()+")");
//        for (int i=0; i<node.getNumEntries(); i++) {
//          Entry e = node.getEntry(i);
//          System.out.println(e + " " + e.getClass() + " , mbr "+e.getMBR());
//        }

        MBR newMbr = node.mbr();
        entry.setMBR(newMbr);
        file.writeNode(parent);
        node = parent;
      }
      else
        node = null;
    }


    try {
      Node root = file.readNode(0);
      root.test();
    }
    catch (RuntimeException e) {
      System.out.println("insert " + o + " " + mbr);
      logger.info("\ninsert " + o + " " + mbr);
      logger.info(this.toString());
      throw e;
    }
    */
  }

 /*
  private synchronized void insert(Node n, int level, final int height, Map<Integer, Boolean> reInsert) {
    logger.info("insert " + n + "\n");

    MBR mbr = n.mbr();
    Node node = chooseSubtree(file.readNode(0), mbr, level + 1, height);
    node.addEntry(n);
    file.writeNode(node);

    // adjust mbrs
    while (node != null) {
      // hasOverflow occured
      if (hasOverflow(node)) {
        Node split = overflowTreatment(node, level++, height, reInsert);
        if (split != null) {
          if (node.getPageID() != 0) {
            Node parent = file.readNode(node.parentID);
            parent.addEntry(split);

            Entry entry1 = parent.entries[node.index];
            entry1.setMBR(node.mbr());
            Entry entry2 = parent.entries[split.index];
            entry2.setMBR(split.mbr());

            file.writeNode(parent);
            node = parent;
          }
          // if root was split: create a new root that points the two split nodes
          else {
            createNewRoot(node, split);
          }
        }
      }
      // no hasOverflow, only adjust mbrs in parent
      else if (node.getPageID() != 0) {
        Node parent = file.readNode(node.getParent());
        Entry entry = parent.getEntry(node.getPlace());
        entry.setMBR(node.mbr());
        file.writeNode(parent);
        node = parent;
      }
      else
        node = null;
    }

    try {
      Node root = file.readNode(0);
      root.test();
    }
    catch (RuntimeException e) {
      logger.info("\ninsert " + n + " " + mbr);
      logger.info(this.toString());
      throw e;
    }
  }

  // Retrieves the leaf node with correct mbr and id
  private Deletion findLeaf(Node node, SpatialData o) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.getEntry(i).getID() == o.getObjectID())
          return new Deletion((LeafNode) node, i);
      }
    }
    else {
      MBR mbr = new MBR(o.getValues(), o.getValues());
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.getEntry(i).getMBR().overlaps(mbr)) {
          Node child = file.readNode(node.getEntry(i).getID());
          return findLeaf(child, o);
        }
      }
    }
    return null;
  }

// condenses the tree after remove of some childNodes
  private void condenseTree(Node node, Stack stack) {
    // node is not root
    if (node.getPageID() != 0) {
      Node p = file.readNode(node.getParent());
      if (node.getNumEntries() < file.getMinimum()) {
        p.deleteEntry(node.getPlace());
        stack.push(node);
      }
      else {
        p.getEntry(node.getPlace()).setMBR(node.mbr());
      }
      file.writeNode(p);
      condenseTree(p, stack);
    }
    // node is root
    else {
      if (node.getNumEntries() == 1 && !node.isLeaf()) {
        Node child = file.readNode(node.getEntry(0).getID());
        Node newRoot = null;
        if (child.isLeaf()) {
          newRoot = new LeafNode(this.file);
          newRoot.setPageID(0);
          for (int i = 0; i < child.getNumEntries(); i++) {
            Entry e = child.getEntry(i);
            DBObject o = new DBObject(e.getID(), e.getMBR().getMin(), 0);
            newRoot.addEntry(o);
          }
        }
        else {
          newRoot = new DirectoryNode(this.file);
          newRoot.setPageID(0);
          for (int i = 0; i < child.getNumEntries(); i++) {
            Entry e = child.getEntry(i);
            Node n = file.readNode(e.getID());
            newRoot.addEntry(n);
          }
        }
        file.writeNode(newRoot);
      }
    }
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   *
   * @param objects the objects to be inserted
   * @return the leaf nodes containing the objects
   */
  private LeafNode[] createLeafNodes(SpatialData[] objects) {
    int minEntries = file.getMinimum();
    int maxEntries = file.getMaximum();

    ArrayList<LeafNode> result = new ArrayList<LeafNode>();
    BulkSplitDescription split = new BulkSplitDescription();

    while (objects.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      split.chooseBulkSplitAxis(objects);
      split.chooseBulkSplitPoint(objects.length, minEntries, maxEntries);
      msg.append("\nsplitAxis " + split.splitAxis);
      msg.append("\nsplitPoint " + split.splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(split.splitAxis);
      Arrays.sort(objects, comp);

      // create node
      LeafNode leafNode = new LeafNode(this.file);
      file.writeNode(leafNode);
      result.add(leafNode);

      // insert data
      for (int i = 0; i < split.splitPoint; i++) {
        leafNode.addEntry(objects[i]);
      }

      // copy array
      SpatialData[] rest = new SpatialData[objects.length - split.splitPoint];
      System.arraycopy(objects, split.splitPoint, rest, 0, objects.length - split.splitPoint);
      objects = rest;
      msg.append("\nrestl. objects # " + objects.length);

      // write to file
      file.writeNode(leafNode);
      msg.append("\npageNo " + leafNode.getPageID());
      logger.fine(msg.toString() + "\n");
    }

    logger.fine("numDataPages = " + result.size());
    return result.toArray(new LeafNode[result.size()]);
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   *
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private DirectoryNode[] createDirectoryNodes(Node[] nodes) {
    int minEntries = file.getMinimum();
    int maxEntries = file.getMaximum();

    ArrayList<DirectoryNode> result = new ArrayList<DirectoryNode>();
    BulkSplitDescription split = new BulkSplitDescription();

    while (nodes.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      split.chooseBulkSplitAxis(nodes);
      split.chooseBulkSplitPoint(nodes.length, minEntries, maxEntries);
      msg.append("\nsplitAxis " + split.splitAxis);
      msg.append("\nsplitPoint " + split.splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(split.splitAxis);
      Arrays.sort(nodes, comp);

      // create node
      DirectoryNode dirNode = new DirectoryNode(this.file);
      file.writeNode(dirNode);
      result.add(dirNode);

      // insert data
      for (int i = 0; i < split.splitPoint; i++) {
        dirNode.addEntry(nodes[i]);
      }

      // copy array
      Node[] rest = new Node[nodes.length - split.splitPoint];
      System.arraycopy(nodes, split.splitPoint, rest, 0, nodes.length - split.splitPoint);
      nodes = rest;
      msg.append("\nrestl. nodes # " + nodes.length);

      // write to file
      file.writeNode(dirNode);
      msg.append("\npageNo " + dirNode.getPageID());
      logger.fine(msg.toString() + "\n");
    }

    logger.info("numDirPages " + result.size());
    return result.toArray(new DirectoryNode[result.size()]);
  }


  /**
   * Returns the root leaf node for bulk load
   *
   * @param root    the new root node
   * @param objects the objects to be inserted
   * @return the root node
   */
  private LeafNode createRoot(LeafNode root, SpatialData[] objects) {
    StringBuffer msg = new StringBuffer();

    // get the split axis and split point
    BulkSplitDescription split = new BulkSplitDescription();
    split.chooseBulkSplitAxis(objects);

    // sort in the right dimension
    final SpatialComparator comp = new SpatialComparator();
    comp.setCompareDimension(split.splitAxis);
    Arrays.sort(objects, comp);

    // insert data
    for (int i = 0; i < objects.length; i++) {
      root.addEntry(objects[i]);
    }

    // write to file
    file.writeNode(root);
    msg.append("\npageNo " + root.getPageID());
    logger.fine(msg.toString() + "\n");

    return root;
  }

  // creates the root node for bulk load
  private DirectoryNode createRoot(DirectoryNode root, Node[] nodes) {
    StringBuffer msg = new StringBuffer();

    // get the split axis and split point
    BulkSplitDescription split = new BulkSplitDescription();
    split.chooseBulkSplitAxis(nodes);

    // sort in the right dimension
    final SpatialComparator comp = new SpatialComparator();
    comp.setCompareDimension(split.splitAxis);
    Arrays.sort(nodes, comp);

    // insert data
    for (int i = 0; i < split.splitPoint; i++) {
      root.addEntry(nodes[i]);
    }

    // write to file
    file.writeNode(root);
    msg.append("\nroot pageNo " + root.getPageID());
    logger.fine(msg.toString() + "\n");

    return root;
  }


  private class Deletion {
    LeafNode leaf;
    int index;

    public Deletion(LeafNode leaf, int index) {
      this.leaf = leaf;
      this.index = index;
    }
  }

  private class Enlargement implements Comparable {
    int pageID;
    double volume;
    double volInc;
    double overlapInc;

    public Enlargement(int pageID, double volume, double volInc, double overlapInc) {
      this.pageID = pageID;
      this.volume = volume;
      this.volInc = volInc;
      this.overlapInc = overlapInc;
    }

    public int compareTo(Object o) {
      Enlargement other = (Enlargement) o;
      if (this.overlapInc < other.overlapInc) return -1;
      if (this.overlapInc > other.overlapInc) return +1;

      if (this.volInc < other.volInc) return -1;
      if (this.volInc > other.volInc) return +1;

      if (this.volume < other.volume) return -1;
      if (this.volume > other.volume) return +1;

      return this.pageID - other.pageID;
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(RTree.class.toString());
    logger.setLevel(loggerLevel);
  }

}
