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
package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract super class for all tree based index classes.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - PageFile
 * @navhas - contains - Node
 * @navhas - - - TreeIndexHeader
 *
 * @param <N> the type of Node used in the index
 * @param <E> the type of Entry used in the index
 */
public abstract class IndexTree<N extends Node<E>, E extends Entry> implements Index {
  /**
   * The file storing the entries of this index.
   */
  private final PageFile<N> file;

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
  private E rootEntry;

  /**
   * Constructor.
   *
   * @param pagefile page file to use
   */
  public IndexTree(PageFile<N> pagefile) {
    super();
    this.file = pagefile;
  }

  /**
   * Initialize the tree if the page file already existed.
   */
  @Override
  public void initialize() {
    TreeIndexHeader header = createHeader();
    if(this.file.initialize(header)) {
      initializeFromFile(header, file);
    }
    rootEntry = createRootEntry();
  }

  /**
   * Get the (STATIC) logger for this class.
   *
   * @return the static logger
   */
  protected abstract Logging getLogger();

  /**
   * Returns the entry representing the root if this index.
   *
   * @return the entry representing the root if this index
   */
  public final E getRootEntry() {
    return rootEntry;
  }

  /**
   * Page ID of the root entry.
   *
   * @return page id
   */
  public final int getRootID() {
    return getPageID(rootEntry);
  }

  /**
   * Reads the root node of this index from the file.
   *
   * @return the root node of this index
   */
  public N getRoot() {
    return file.readPage(getPageID(rootEntry));
  }

  /**
   * Test if a given ID is the root.
   *
   * @param page Page to test
   * @return Whether the page ID is the root
   */
  protected boolean isRoot(N page) {
    return getRootID() == page.getPageID();
  }

  /**
   * Convert a directory entry to its page id.
   *
   * @param entry Entry
   * @return Page ID
   */
  protected int getPageID(Entry entry) {
    if(entry instanceof LeafEntry) {
      throw new AbortException("Leafs do not have page ids!");
    }
    return ((DirectoryEntry) entry).getPageID();
  }

  /**
   * Returns the node with the specified id.
   *
   * @param nodeID the page id of the node to be returned
   * @return the node with the specified id
   */
  public N getNode(int nodeID) {
    if(nodeID == getPageID(rootEntry)) {
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
    return getNode(getPageID(entry));
  }

  /**
   * Write a node to the backing storage.
   *
   * @param node Node to write
   */
  protected void writeNode(N node) {
    file.writePage(node);
  }

  /**
   * Delete a node from the backing storage.
   *
   * @param node Node to delete
   */
  protected void deleteNode(N node) {
    file.deletePage(node.getPageID());
  }

  /**
   * Creates a header for this index structure which is an instance of
   * {@link TreeIndexHeader}. Subclasses may need to overwrite this method if
   * they need a more specialized header.
   *
   * @return a new header for this index structure
   */
  protected TreeIndexHeader createHeader() {
    return new TreeIndexHeader(file.getPageSize(), dirCapacity, leafCapacity, dirMinimum, leafMinimum);
  }

  /**
   * Initializes this index from an existing persistent file.
   *
   * @param header File header
   * @param file Page file
   */
  public void initializeFromFile(TreeIndexHeader header, PageFile<N> file) {
    this.dirCapacity = header.getDirCapacity();
    this.leafCapacity = header.getLeafCapacity();
    this.dirMinimum = header.getDirMinimum();
    this.leafMinimum = header.getLeafMinimum();

    if(getLogger().isDebugging()) {
      StringBuilder msg = new StringBuilder();
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
    initializeCapacities(exampleLeaf);

    // create empty root
    createEmptyRoot(exampleLeaf);

    final Logging log = getLogger();
    if(log.isStatistics()) {
      String cls = this.getClass().getName();
      log.statistics(new LongStatistic(cls + ".directory.capacity", dirCapacity));
      log.statistics(new LongStatistic(cls + ".directory.minfill", dirMinimum));
      log.statistics(new LongStatistic(cls + ".leaf.capacity", leafCapacity));
      log.statistics(new LongStatistic(cls + ".leaf.minfill", leafMinimum));
    }

    initialized = true;
  }

  /**
   * Returns the path to the root of this tree.
   *
   * @return the path to the root of this tree
   */
  public final IndexTreePath<E> getRootPath() {
    return new IndexTreePath<>(null, rootEntry, -1);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param exampleLeaf an object that will be stored in the index
   */
  protected abstract void initializeCapacities(E exampleLeaf);

  /**
   * Creates an empty root node and writes it to file.
   *
   * @param exampleLeaf an object that will be stored in the index
   */
  protected abstract void createEmptyRoot(E exampleLeaf);

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected abstract E createRootEntry();

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  protected abstract N createNewLeafNode();

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  protected abstract N createNewDirectoryNode();

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(E entry) {
    // Default is no-op.
  }

  /**
   * Performs necessary operations after deleting the specified entry.
   *
   * @param entry the entry that was removed
   */
  protected void postDelete(E entry) {
    // Default is no-op.
  }

  /**
   * Log some statistics, if enabled.
   */
  @Override
  public void logStatistics() {
    file.logStatistics();
  }

  /**
   * Get the page size of the backing storage.
   *
   * @return Page size
   */
  protected int getPageSize() {
    return file.getPageSize();
  }

  /**
   * Get the minimum fill of a directory page (except root).
   *
   * @return Minimum fill
   */
  public int getDirMinimum() {
    return dirMinimum;
  }

  /**
   * Get the minimum fill of a leaf page (except root).
   *
   * @return Minimum fill
   */
  public int getLeafMinimum() {
    return leafMinimum;
  }

  /**
   * Directly access the backing page file.
   *
   * @return the page file
   */
  @Deprecated
  protected PageFile<N> getFile() {
    return file;
  }
}
