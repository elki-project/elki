package de.lmu.ifi.dbs.elki.persistent;
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

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a PageFile that simulates I/O-access.<br>
 * Implemented as a Map with keys representing the ids of the saved pages.
 * 
 * @author Elke Achtert
 * 
 * @param <P> Page type
 */
public class MemoryPageFile<P extends Page> extends AbstractStoringPageFile<P> {
  /**
   * Holds the pages.
   */
  private final Map<Integer, P> file;

  /**
   * Creates a new MemoryPageFile that is supported by a cache with the
   * specified parameters.
   * 
   * @param pageSize the size of a page in Bytes
   */
  public MemoryPageFile(int pageSize) {
    super(pageSize);
    this.file = new HashMap<Integer, P>();
  }

  @Override
  public synchronized P readPage(int pageID) {
    readAccess++;
    return file.get(pageID);
  }
  
  @Override
  protected void writePage(Integer pageID, P page) {
    writeAccess++;
    file.put(pageID, page);
    page.setDirty(false);
  }

  @Override
  public synchronized void deletePage(int pageID) {
    // put id to empty nodes and
    // delete from cache
    super.deletePage(pageID);

    // delete from file
    writeAccess++;
    file.remove(pageID);
  }

  @Override
  public void clear() {
    file.clear();
  }
}