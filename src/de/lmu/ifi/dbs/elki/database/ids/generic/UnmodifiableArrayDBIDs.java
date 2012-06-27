package de.lmu.ifi.dbs.elki.database.ids.generic;

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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayStaticDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.utilities.iterator.UnmodifiableIterator;

/**
 * Unmodifiable wrapper for DBIDs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.ids.DBIDs
 */
public class UnmodifiableArrayDBIDs implements ArrayStaticDBIDs {
  /**
   * The DBIDs we wrap.
   */
  final private ArrayDBIDs inner;

  /**
   * Constructor.
   * 
   * @param inner Inner DBID collection.
   */
  public UnmodifiableArrayDBIDs(ArrayDBIDs inner) {
    super();
    this.inner = inner;
  }

  @Override
  public boolean contains(DBID o) {
    return inner.contains(o);
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @SuppressWarnings("deprecation")
  @Override
  public Iterator<DBID> iterator() {
    return new UnmodifiableIterator<DBID>(inner.iterator());
  }
  
  @Override
  public DBIDIter iter() {
    return inner.iter();
  }

  @Override
  public int size() {
    return inner.size();
  }

  /**
   * Returns a string representation of the inner DBID collection.
   */
  @Override
  public String toString() {
    return inner.toString();
  }

  @Override
  public DBID get(int i) {
    return inner.get(i);
  }

  @Override
  public int binarySearch(DBID key) {
    return inner.binarySearch(key);
  }
}