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

import de.lmu.ifi.dbs.elki.logging.Logging;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * A memory based implementation of a PageFile that simulates I/O-access.
 * Implemented as a Map with keys representing the ids of the saved pages.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @param <P> Page type
 */
public class MemoryPageFile<P extends Page> extends AbstractStoringPageFile<P> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MemoryPageFile.class);

  /**
   * Holds the pages.
   */
  private final Int2ObjectOpenHashMap<P> file;

  /**
   * Creates a new MemoryPageFile that is supported by a cache with the
   * specified parameters.
   *
   * @param pageSize the size of a page in Bytes
   */
  public MemoryPageFile(int pageSize) {
    super(pageSize);
    this.file = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public synchronized P readPage(int pageID) {
    countRead();
    return file.get(pageID);
  }

  @Override
  protected void writePage(int pageID, P page) {
    countWrite();
    file.put(pageID, page);
    page.setDirty(false);
  }

  @Override
  public synchronized void deletePage(int pageID) {
    // put id to empty nodes and
    // delete from cache
    super.deletePage(pageID);

    // delete from file
    countWrite();
    file.remove(pageID);
  }

  @Override
  public void clear() {
    file.clear();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
