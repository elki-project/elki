package de.lmu.ifi.dbs.elki.database.ids;

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

import java.util.Iterator;

/**
 * Interface for a collection of database references (IDs).
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf DBID
 */
public interface DBIDs extends Iterable<DBID> {
  /**
   * Retrieve Iterator access to the IDs.
   * 
   * @return an iterator for the IDs
   */
  @Override
  public Iterator<DBID> iterator();

  /**
   * Get a DBIDIterator (a more efficient API).
   * 
   * @return iterator
   */
  public DBIDIter iter();

  /**
   * Retrieve the collection / data size.
   * 
   * @return collection size
   */
  public int size();

  /**
   * Test whether an ID is contained.
   * 
   * @param o object to test
   * @return true when contained
   */
  public boolean contains(DBID o);

  /**
   * Test for an empty DBID collection.
   * 
   * @return true when empty.
   */
  public boolean isEmpty();
}
