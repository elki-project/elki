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
package de.lmu.ifi.dbs.elki.persistent;

import java.util.Stack;

import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;

/**
 * Abstract class implementing general methods of a PageFile. A PageFile stores
 * objects that implement the <code>Page</code> interface.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @param <P> Page type
 */
public abstract class AbstractStoringPageFile<P extends Page> extends AbstractPageFile<P> {
  /**
   * A stack holding the empty page ids.
   */
  protected Stack<Integer> emptyPages;

  /**
   * The last page ID.
   */
  protected int nextPageID;

  /**
   * The size of a page in Bytes.
   */
  protected int pageSize;

  /**
   * Creates a new PageFile.
   */
  protected AbstractStoringPageFile(int pageSize) {
    this.emptyPages = new Stack<>();
    this.nextPageID = 0;
    this.pageSize = pageSize;
  }

  /**
   * Sets the id of the given page.
   * 
   * @param page the page to set the id
   */
  @Override
  public int setPageID(P page) {
    int pageID = page.getPageID();
    if(pageID == -1) {
      pageID = getNextEmptyPageID();
      if(pageID == -1) {
        pageID = nextPageID++;
      }
      page.setPageID(pageID);
    }
    return pageID;
  }

  /**
   * Deletes the node with the specified id from this file.
   * 
   * @param pageID the id of the node to be deleted
   */
  @Override
  public void deletePage(int pageID) {
    // put id to empty nodes
    emptyPages.push(pageID);
  }

  /**
   * Returns the next empty page id.
   * 
   * @return the next empty page id
   */
  private int getNextEmptyPageID() {
    if(!emptyPages.empty()) {
      return emptyPages.pop();
    }
    else {
      return -1;
    }
  }

  /**
   * Returns the next page id.
   * 
   * @return the next page id
   */
  @Override
  public int getNextPageID() {
    return nextPageID;
  }

  /**
   * Sets the next page id.
   * 
   * @param nextPageID the next page id to be set
   */
  @Override
  public void setNextPageID(int nextPageID) {
    this.nextPageID = nextPageID;
  }

  /**
   * Get the page size of this page file.
   * 
   * @return page size
   */
  @Override
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Initialize the page file with the given header - return "true" if the file
   * already existed.
   * 
   * @param header Header
   * @return true when the file already existed.
   */
  @Override
  public boolean initialize(PageHeader header) {
    this.pageSize = header.getPageSize();
    return false;
  }

  @Override
  public void logStatistics() {
    super.logStatistics();
    if (getLogger().isStatistics()) {
      getLogger().statistics(new LongStatistic(this.getClass().getName() + ".numpages", nextPageID - emptyPages.size()));
    }
  }
}