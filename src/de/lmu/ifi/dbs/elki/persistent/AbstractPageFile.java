package de.lmu.ifi.dbs.elki.persistent;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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


/**
 * Abstract base class for the page file API for both caches and true page files
 * (in-memory and on-disk).
 * 
 * @author Erich Schubert
 * 
 * @param <P> page type
 */
public abstract class AbstractPageFile<P extends Page> implements PageFile<P> {
  /**
   * The read I/O-Access of this file.
   */
  protected long readAccess;

  /**
   * The write I/O-Access of this file.
   */
  protected long writeAccess;

  /**
   * Constructor.
   */
  public AbstractPageFile() {
    super();
    this.readAccess = 0;
    this.writeAccess = 0;
  }

  /**
   * Writes a page into this file. The method tests if the page has already an
   * id, otherwise a new id is assigned and returned.
   * 
   * @param page the page to be written
   * @return the id of the page
   */
  @Override
  public final synchronized int writePage(P page) {
    int pageid = setPageID(page);
    writePage(pageid, page);
    return pageid;
  }

  /**
   * Perform the actual page write operation.
   * 
   * @param pageid Page id
   * @param page Page to write
   */
  protected abstract void writePage(int pageid, P page);

  @Override
  public void close() {
    clear();
  }

  @Override
  public final long getReadOperations() {
    return readAccess;
  }

  @Override
  public final long getWriteOperations() {
    return writeAccess;
  }

  @Override
  public final void resetPageAccess() {
    this.readAccess = 0;
    this.writeAccess = 0;
  }
}