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

/**
 * Page file interface.
 * 
 * @author Erich Schubert
 * @since 0.1
 * 
 * @has - - - Page
 * 
 * @param <P> Page file
 */
public interface PageFile<P extends Page> {
  /**
   * Sets the id of the given page.
   * 
   * @param page the page to set the id
   * @return the page id
   */
  int setPageID(P page);

  /**
   * Writes a page into this file. The method tests if the page has already an
   * id, otherwise a new id is assigned and returned.
   * 
   * @param page the page to be written
   * @return the id of the page
   */
  int writePage(P page);

  /**
   * Reads the page with the given id from this file.
   * 
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  P readPage(int pageID);

  /**
   * Deletes the node with the specified id from this file.
   * 
   * @param pageID the id of the node to be deleted
   */
  void deletePage(int pageID);

  /**
   * Closes this file.
   */
  void close();

  /**
   * Clears this PageFile.
   */
  void clear();

  /**
   * Returns the next page id.
   * 
   * @return the next page id
   */
  int getNextPageID();

  /**
   * Sets the next page id.
   * 
   * @param nextPageID the next page id to be set
   */
  void setNextPageID(int nextPageID);

  /**
   * Get the page size of this page file.
   * 
   * @return page size
   */
  int getPageSize();

  /**
   * Initialize the page file with the given header - return "true" if the file
   * already existed.
   * 
   * @param header Header
   * @return true when the file already existed.
   */
  boolean initialize(PageHeader header);

  /**
   * Log some statistics to the appropriate logger.
   */
  void logStatistics();
}
