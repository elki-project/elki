package de.lmu.ifi.dbs.elki.database.ids.generic;
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

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;


/**
 * Merge the IDs of multiple layers into one.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.ids.DBIDs
 */
// TODO: include ID mapping?
public class MergedDBIDs implements DBIDs, Collection<DBID> {
  /**
   * Childs to merge
   */
  DBIDs childs[];
  
  /**
   * Constructor.
   * 
   * @param childs
   */
  public MergedDBIDs(DBIDs... childs) {
    super();
    this.childs = childs;
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public Iterator<DBID> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int size() {
    int si = 0;
    for(DBIDs child : childs) {
      si += child.size();
    }
    return si;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Object[] toArray() {
    return toArray(new Object[size()]);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    final int si = size();
    T[] r = a;
    if(a.length < si) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), si);
    }
    int i = 0;
    for (Iterator<DBID> iter = iterator(); iter.hasNext(); i++) {
      DBID id = iter.next();
      r[i] = (T) id;
    }
    // zero-terminate array
    if(r.length > si) {
      r[si] = null;
    }
    return r;
  }

  @Override
  public boolean contains(Object o) {
    for(DBIDs child : childs) {
      if(child.contains(o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    Iterator<?> e = c.iterator();
    while(e.hasNext()) {
      if(!contains(e.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean add(DBID e) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean addAll(Collection<? extends DBID> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }
}
