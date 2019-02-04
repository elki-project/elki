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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.util.NoSuchElementException;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Implementation using Fastutil IntSet.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - Itr
 */
class FastutilIntOpenHashSetModifiableDBIDs implements HashSetModifiableDBIDs, IntegerDBIDs {
  /**
   * The actual store.
   */
  IntOpenHashSet store;

  /**
   * Customized table.
   *
   * @author Erich Schubert
   */
  private static class IntOpenHashSet extends it.unimi.dsi.fastutil.ints.IntOpenHashSet {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public IntOpenHashSet() {
      super();
    }

    /**
     * Constructor.
     *
     * @param size Size
     */
    public IntOpenHashSet(int size) {
      super(size);
    }

    /**
     * Scan position for pop().
     */
    private transient int c = -1;

    /**
     * Pop a single value from the hash table.
     *
     * @return Value removed.
     */
    public int popInt() {
      if(size == 0) {
        throw new NoSuchElementException();
      }
      if(containsNull) {
        containsNull = false;
        --size;
        return 0;
      }
      final int key[] = this.key;
      int k, pos = c < key.length ? c : key.length;
      for(;;) {
        if(pos <= 0)
          pos = key.length;
        k = key[--pos];
        if(k != 0) {
          size--;
          shiftKeys(pos);
          if(size < maxFill >> 2 && n > DEFAULT_INITIAL_SIZE) {
            rehash(n >> 1);
          }
          c = pos;
          return k;
        }
      }
    }
  }

  /**
   * Constructor.
   *
   * @param size Initial size
   */
  protected FastutilIntOpenHashSetModifiableDBIDs(int size) {
    super();
    this.store = new IntOpenHashSet(size);
  }

  /**
   * Constructor.
   */
  protected FastutilIntOpenHashSetModifiableDBIDs() {
    super();
    this.store = new IntOpenHashSet();
  }

  /**
   * Constructor.
   *
   * @param existing Existing IDs
   */
  protected FastutilIntOpenHashSetModifiableDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
  }

  @Override
  public Itr iter() {
    return new Itr(store.iterator());
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    // TODO: re-add: store.ensureCapacity(ids.size());
    boolean success = false;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      success |= store.add(DBIDUtil.asInteger(iter));
    }
    return success;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      success |= store.remove(DBIDUtil.asInteger(id));
    }
    return success;
  }

  @Override
  public boolean add(DBIDRef e) {
    return store.add(DBIDUtil.asInteger(e));
  }

  @Override
  public boolean remove(DBIDRef o) {
    return store.remove(DBIDUtil.asInteger(o));
  }

  @Override
  public boolean retainAll(DBIDs set) {
    boolean modified = false;
    for(DBIDMIter it = iter(); it.valid(); it.advance()) {
      if(!set.contains(it)) {
        it.remove();
        modified = true;
      }
    }
    return modified;
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
    return store.contains(DBIDUtil.asInteger(o));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append('[');
    for(DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if(buf.length() > 1) {
        buf.append(", ");
      }
      buf.append(iter.toString());
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public DBIDVar pop(DBIDVar outvar) {
    if(store.size() == 0) {
      throw new NoSuchElementException("Cannot pop() from an empty array.");
    }
    final int val = store.popInt();
    if(outvar instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) outvar).internalSetIndex(val);
    }
    else { // Fallback, should not happen (more expensive).
      outvar.set(DBIDUtil.importInteger(val));
    }
    store.remove(val);
    return outvar;
  }

  /**
   * Iterator over Fastutil hashs.
   *
   * @author Erich Schubert
   */
  protected static class Itr implements IntegerDBIDMIter {
    /**
     * The actual iterator.
     */
    IntIterator it;

    /**
     * Current value.
     */
    int prev;

    /**
     * Constructor.
     *
     * @param it Int set iterator
     */
    public Itr(IntIterator it) {
      super();
      if(it != null && it.hasNext()) {
        this.it = it;
        this.prev = it.nextInt();
      }
    }

    @Override
    public boolean valid() {
      return it != null;
    }

    @Override
    public IntegerDBIDMIter advance() {
      if(it != null && it.hasNext()) {
        prev = it.nextInt();
      }
      else {
        it = null;
      }
      return this;
    }

    @Override
    public int internalGetIndex() {
      return prev;
    }

    @Override
    public String toString() {
      return Integer.toString(internalGetIndex());
    }

    @Override
    public void remove() {
      it.remove();
    }
  }
}
