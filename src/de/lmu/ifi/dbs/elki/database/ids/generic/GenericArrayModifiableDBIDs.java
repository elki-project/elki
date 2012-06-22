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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Array-oriented implementation of a modifiable DBID collection.
 * 
 * This should only be instantiated by a
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory}!
 * 
 * Use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newArray}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DBID
 */
public class GenericArrayModifiableDBIDs extends ArrayList<DBID> implements ArrayModifiableDBIDs {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericArrayModifiableDBIDs(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor without extra hints
   */
  public GenericArrayModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericArrayModifiableDBIDs(DBIDs c) {
    super(c.size());
    addDBIDs(c);
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    super.ensureCapacity(size() + ids.size());
    boolean changed = false;
    for(DBID id : ids) {
      changed |= add(id);
    }
    return changed;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean changed = false;
    for(DBID id : ids) {
      changed |= super.remove(id);
    }
    return changed;
  }

  @Override
  public boolean remove(DBID id) {
    return super.remove(id);
  }

  @Override
  public void sort() {
    Collections.sort(this);
  }

  @Override
  public void sort(Comparator<? super DBID> comparator) {
    Collections.sort(this, comparator);
  }

  @Override
  public DBIDIter iter() {
    return new DBIDIterAdapter(iterator());
  }

  @Override
  public int binarySearch(DBID key) {
    return Collections.binarySearch(this, key);
  }

  @Override
  public boolean contains(DBID o) {
    return super.contains(o);
  }

  @Override
  public void swap(int a, int b) {
    set(a, set(b, get(a)));
  }
}