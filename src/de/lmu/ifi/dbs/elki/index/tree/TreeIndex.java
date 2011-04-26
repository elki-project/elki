package de.lmu.ifi.dbs.elki.index.tree;

import java.io.File;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.LRUCache;
import de.lmu.ifi.dbs.elki.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.persistent.PersistentPageFile;

/**
 * Abstract super class for all tree based index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Node oneway - - contains
 * @apiviz.has TreeIndexHeader oneway
 * 
 * @param <O> the type of DatabaseObject to be stored in the index
 * @param <N> the type of Node used in the index
 * @param <E> the type of Entry used in the index
 */
public abstract class TreeIndex<O, N extends Node<N, E>, E extends Entry> extends AbstractIndex<O> {
  /**
   * Holds the name of the file storing the index or null
   */
  private String fileName = null;

  /**
   * Holds the index page size
   */
  protected int pageSize;

  /**
   * Holds the cache size
   */
  protected long cacheSize;

  /**
   * The file storing the entries of this index.
   */
  protected PageFile<N> file;

  /**
   * True if this index is already initialized.
   */
  protected boolean initialized = false;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a
   * directory node).
   */
  protected int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf
   * node).
   */
  protected int leafCapacity;

  /**
   * The minimum number of entries in a directory node.
   */
  protected int dirMinimum;

  /**
   * The minimum number of entries in a leaf node.
   */
  protected int leafMinimum;

  /**
   * The entry representing the root node.
   */
  private E rootEntry = createRootEntry();
  
  /**
   * Constructor.
   * 
   * @param relation Representation
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   */
  public TreeIndex(Relation<O> relation, String fileName, int pageSize, long cacheSize) {
    super(relation);
    this.fileName = fileName;
    this.pageSize = pageSize;
    this.cacheSize = cacheSize;
  }

  /**
   * Get the (STATIC) logger for this class.
   * 
   * @return the static logger
   */
  abstract protected Logging getLogger();

  /**
   * Returns the entry representing the root if this index.
   * 
   * @return the entry representing the root if this index
   */
  public final E getRootEntry() {
    return rootEntry;
  }

  /**
   * Returns the node with the specified id.
   * 
   * @param nodeID the page id of the node to be returned
   * @return the node with the specified id
   */
  public N getNode(Integer nodeID) {
    if(nodeID == rootEntry.getEntryID()) {
      return getRoot();
    }
    else {
      return file.readPage(nodeID);
    }
  }

  /**
   * Returns the node that is represented by the specified entry.
   * 
   * @param entry the entry representing the node to be returned
   * @return the node that is represented by the specified entry
   */
  public final N getNode(E entry) {
    return getNode(entry.getEntryID());
  }

  /**
   * Creates a header for this index structure which is an instance of
   * {@link TreeIndexHeader}. Subclasses may need to overwrite this method if
   * they need a more specialized header.
   * 
   * @return a new header for this index structure
   */
  protected TreeIndexHeader createHeader() {
    return new TreeIndexHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
  }

  /**
   * Initializes this index from an existing persistent file.
   */
  public void initializeFromFile() {
    if(fileName == null) {
      throw new IllegalArgumentException("Parameter file name is not specified.");
    }

    // init the file
    TreeIndexHeader header = createHeader();
    this.file = new PersistentPageFile<N>(header, cacheSize, new LRUCache<N>(), fileName, getNodeClass());

    this.dirCapacity = header.getDirCapacity();
    this.leafCapacity = header.getLeafCapacity();
    this.dirMinimum = header.getDirMinimum();
    this.leafMinimum = header.getLeafMinimum();

    if(getLogger().isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass());
      msg.append("\n file = ").append(file.getClass());
      getLogger().debugFine(msg.toString());
    }

    this.initialized = true;
  }

  /**
   * Initializes the index.
   * 
   * @param exampleLeaf an object that will be stored in the index
   */
  protected final void initialize(E exampleLeaf) {
    // determine minimum and maximum entries in a node
    // todo verbose flag as parameter
    // initializeCapacities(object, true);
    initializeCapacities(exampleLeaf);

    // init the file
    if(fileName == null) {
      this.file = new MemoryPageFile<N>(pageSize, cacheSize, new LRUCache<N>());
    }
    else {
      if(new File(fileName).exists()) {
        initializeFromFile();
      }
      else {
        this.file = new PersistentPageFile<N>(createHeader(), cacheSize, new LRUCache<N>(), fileName, getNodeClass());
      }
    }

    // create empty root
    createEmptyRoot(exampleLeaf);

    if(getLogger().isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass()).append("\n");
      msg.append(" file    = ").append(file.getClass()).append("\n");
      msg.append(" maximum number of dir entries = ").append((dirCapacity - 1)).append("\n");
      msg.append(" minimum number of dir entries = ").append(dirMinimum).append("\n");
      msg.append(" maximum number of leaf entries = ").append((leafCapacity - 1)).append("\n");
      msg.append(" minimum number of leaf entries = ").append(leafMinimum).append("\n");
      msg.append(" root    = ").append(getRoot());
      getLogger().debugFine(msg.toString());
    }

    initialized = true;
  }

  /**
   * Returns the path to the root of this tree.
   * 
   * @return the path to the root of this tree
   */
  protected final TreeIndexPath<E> getRootPath() {
    return new TreeIndexPath<E>(new TreeIndexPathComponent<E>(rootEntry, null));
  }

  /**
   * Reads the root node of this index from the file.
   * 
   * @return the root node of this index
   */
  protected N getRoot() {
    return file.readPage(rootEntry.getEntryID());
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   * 
   * @param exampleLeaf an object that will be stored in the index
   */
  abstract protected void initializeCapacities(E exampleLeaf);

  /**
   * Creates an empty root node and writes it to file.
   * 
   * @param exampleLeaf an object that will be stored in the index
   */
  abstract protected void createEmptyRoot(E exampleLeaf);

  /**
   * Creates an entry representing the root node.
   * 
   * @return an entry representing the root node
   */
  abstract protected E createRootEntry();

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  abstract protected N createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  abstract protected N createNewDirectoryNode(int capacity);

  /**
   * Performs necessary operations before inserting the specified entry.
   * 
   * @param entry the entry to be inserted
   */
  abstract protected void preInsert(E entry);

  /**
   * Performs necessary operations after deleting the specified object.
   * 
   * @param id object id
   */
  abstract protected void postDelete(DBID id);

  /**
   * Get the node class of this index
   * 
   * @return node class
   */
  abstract protected Class<N> getNodeClass();

  /**
   * @return filename of the underlying persistence layer
   */
  public String getFileName() {
    return fileName;
  }

  @Override
  public PageFileStatistics getPageFileStatistics() {
    return file;
  }
}