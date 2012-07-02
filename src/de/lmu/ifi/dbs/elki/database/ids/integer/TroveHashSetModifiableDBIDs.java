package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.util.Iterator;

import gnu.trove.impl.hash.THashPrimitiveIterator;
import gnu.trove.impl.hash.TIntHash;
import gnu.trove.set.hash.TIntHashSet;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;

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
 * Implementation using GNU Trove Int Hash Sets.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 * @apiviz.has TroveIteratorAdapter
 */
class TroveHashSetModifiableDBIDs implements HashSetModifiableDBIDs {
  /**
   * The actual store.
   */
  TIntHashSet store;

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  protected TroveHashSetModifiableDBIDs(int size) {
    super();
    this.store = new TIntHashSet(size);
  }

  /**
   * Constructor.
   */
  protected TroveHashSetModifiableDBIDs() {
    super();
    this.store = new TIntHashSet();
  }

  /**
   * Constructor.
   * 
   * @param existing Existing IDs
   */
  protected TroveHashSetModifiableDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
  }

  @Override
  public DBIDMIter iter() {
    return new DBIDItr(store);
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      success |= store.add(DBIDFactory.FACTORY.asInteger(iter));
    }
    return success;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBID id : ids) {
      success |= store.remove(DBIDFactory.FACTORY.asInteger(id));
    }
    return success;
  }

  @Override
  public boolean add(DBIDRef e) {
    return store.add(DBIDFactory.FACTORY.asInteger(e));
  }

  @Override
  public boolean remove(DBIDRef o) {
    return store.remove(DBIDFactory.FACTORY.asInteger(o));
  }

  @Override
  public boolean retainAll(DBIDs set) {
    boolean modified = false;
    Iterator<DBID> it = iterator();
    while(it.hasNext()) {
      if(!set.contains(it.next())) {
        it.remove();
        modified = true;
      }
    }
    return modified;
  }

  @Override
  public Iterator<DBID> iterator() {
    return new TroveIteratorAdapter(store.iterator());
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public boolean isEmpty() {
    return store.isEmpty();
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  public boolean contains(DBIDRef o) {
    return store.contains(DBIDFactory.FACTORY.asInteger(o));
  }

  /**
   * Iterator over trove hashs.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class DBIDItr extends THashPrimitiveIterator implements DBIDMIter, IntegerDBIDRef {
    /**
     * The has we access
     */
    private TIntHash hash;

    /**
     * Constructor.
     * 
     * @param hash Trove hash
     */
    public DBIDItr(TIntHash hash) {
      super(hash);
      this.hash = hash;
      this._index = nextIndex(); // Find first element
    }

    @Override
    public boolean valid() {
      return _index >= 0;
    }

    @Override
    public void advance() {
      this._index = nextIndex();
    }

    @Override
    public int getIntegerID() {
      return hash._set[_index];
    }

    @Override
    public DBID getDBID() {
      return new IntegerDBID(hash._set[_index]);
    }
  }
}