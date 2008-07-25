package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.persistent.LRUCache;
import de.lmu.ifi.dbs.elki.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.io.File;

/**
 * Abstract super class for all tree based index classes.
 *
 * @author Elke Achtert 
 */
public abstract class TreeIndex<O extends DatabaseObject, N extends Node<N, E>, E extends Entry>
    extends AbstractParameterizable implements Index<O> {

  /**
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "a file name specifying the name of the file storing the index. "
                                           + "If this parameter is not set the index is hold in the main memory.";

  /**
   * The default pagesize.
   */
  public static final int DEFAULT_PAGE_SIZE = 4000;

  /**
   * Option string for parameter pagesize.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "a positive integer value specifying the size of a page in bytes "
                                           + "(default is " + DEFAULT_PAGE_SIZE + " Byte)";

  /**
   * The default cachesize.
   */
  public static final int DEFAULT_CACHE_SIZE = Integer.MAX_VALUE;

  /**
   * Option string for parameter cachesize.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_SIZE_D = "a positive integer value specifying the size of the cache in bytes "
                                            + "(default is Integer.MAX_VALUE)";

  /**
   * The name of the file for storing the index.
   */
  private String fileName;

  /**
   * The size of a page in bytes.
   */
  protected int pageSize;

  /**
   * The size of the cache in bytes.
   */
  protected int cacheSize;

  /**
   * The file storing the entries of this index.
   */
  protected PageFile<N> file;

  /**
   * True if this index is already initialized.
   */
  protected boolean initialized = false;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a directory node).
   */
  protected int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf node).
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
   * Sets parameters file, pageSize and cacheSize.
   */
  public TreeIndex() {
    super();
    FileParameter fileName = new FileParameter(FILE_NAME_P, FILE_NAME_D,
        FileParameter.FileType.OUTPUT_FILE);
    fileName.setOptional(true);
    optionHandler.put(fileName);

    IntParameter pageSize = new IntParameter(PAGE_SIZE_P, PAGE_SIZE_D, new GreaterConstraint(0));
    pageSize.setDefaultValue(DEFAULT_PAGE_SIZE);
    optionHandler.put(pageSize);

    IntParameter cacheSize = new IntParameter(CACHE_SIZE_P, CACHE_SIZE_D, new GreaterEqualConstraint(0));
    cacheSize.setDefaultValue(DEFAULT_CACHE_SIZE);
    optionHandler.put(cacheSize);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    // filename
    if (optionHandler.isSet(FILE_NAME_P)) {
      fileName = ((File) optionHandler.getOptionValue(FILE_NAME_P)).getPath();
    }
    else {
      fileName = null;
    }

    // pagesize
    pageSize = (Integer) optionHandler.getOptionValue(PAGE_SIZE_P);

    // cachesize
    cacheSize = (Integer) optionHandler.getOptionValue(CACHE_SIZE_P);

    return remainingParameters;
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String parameterDescription() {
    return optionHandler.usage("", false);
  }

  /**
   * Returns the physical read access of this index.
   */
  public final long getPhysicalReadAccess() {
    return file.getPhysicalReadAccess();
  }

  /**
   * Returns the physical write access of this index.
   */
  public final long getPhysicalWriteAccess() {
    return file.getPhysicalWriteAccess();
  }

  /**
   * Returns the logical page access of this index.
   */
  public final long getLogicalPageAccess() {
    return file.getLogicalPageAccess();
  }

  /**
   * Resets the counters for page access.
   */
  public final void resetPageAccess() {
    file.resetPageAccess();
  }

  /**
   * Closes this index and the underlying file.
   * If this index has a oersistent file, all entries are written to disk.
   */
  public final void close() {
    file.close();
  }

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
  public final N getNode(int nodeID) {
    if (nodeID == rootEntry.getID())
      return getRoot();
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
    return getNode(entry.getID());
  }

  /**
   * Creates a header for this index structure.
   * Subclasses may need to overwrite this method.
   *
   * @return a header for this index structure
   */
  protected TreeIndexHeader createHeader() {
    return new TreeIndexHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
  }

  /**
   * Initializes this index from an existing persistent file.
   */
  protected void initializeFromFile() {
    if (fileName == null)
      throw new IllegalArgumentException("Parameter file name is not specified.");

    // init the file
    TreeIndexHeader header = createHeader();
    this.file = new PersistentPageFile<N>(header, cacheSize, new LRUCache<N>(), fileName);

    this.dirCapacity = header.getDirCapacity();
    this.leafCapacity = header.getLeafCapacity();
    this.dirMinimum = header.getDirMinimum();
    this.leafMinimum = header.getLeafMinimum();

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass());
      msg.append("\n file = ").append(file.getClass());
      debugFine(msg.toString());
    }

    this.initialized = true;
  }

  /**
   * Initializes the index.
   *
   * @param object an object that will be stored in the index
   */
  protected final void initialize(O object) {
    // determine minimum and maximum entries in a node
    // todo verbose flag
//    initializeCapacities(object, true);
    initializeCapacities(object, false);

    // init the file
    if (fileName == null) {
      this.file = new MemoryPageFile<N>(pageSize,
                                        cacheSize,
                                        new LRUCache<N>());
    }
    else {
      this.file = new PersistentPageFile<N>(createHeader(),
                                            cacheSize,
                                            new LRUCache<N>(),
                                            fileName);
    }

    // create empty root
    createEmptyRoot(object);

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass()).append("\n");
      msg.append(" file    = ").append(file.getClass()).append("\n");
      msg.append(" maximum number of dir entries = ").append((dirCapacity - 1)).append("\n");
      msg.append(" minimum number of dir entries = ").append(dirMinimum).append("\n");
      msg.append(" maximum number of leaf entries = ").append((leafCapacity - 1)).append("\n");
      msg.append(" minimum number of leaf entries = ").append(leafMinimum).append("\n");
      msg.append(" root    = ").append(getRoot());
      debugFine(msg.toString());
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
    return file.readPage(rootEntry.getID());
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param object  an object that will be stored in the index
   * @param verbose flag to allow verbose messages
   */
  abstract protected void initializeCapacities(O object, boolean verbose);

  /**
   * Creates an empty root node and writes it to file.
   *
   * @param object an object that will be stored in the index
   */
  abstract protected void createEmptyRoot(O object);

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
   * @param o the object to be deleted
   */
  abstract protected void postDelete(O o);
}