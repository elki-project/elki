package de.lmu.ifi.dbs.elki.utilities.datastructures.hash;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.impl.hash.TObjectHash;

import java.util.Arrays;

/**
 * This hash set is designed to keep only a unique copy of each object (hence
 * its name). For this, the method {@link #addOrGet} is the key API, which
 * allows retrieving existing values.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <E>
 */
public class Unique<E> extends TObjectHash<E> {
  /**
   * Serial version number.
   */
  static final long serialVersionUID = 1L;

  /**
   * Constructor with default size and load factors.
   */
  public Unique() {
    super();
  }

  /**
   * Constructor with desired initial size.
   *
   * @param initialCapacity desired initial size.
   */
  public Unique(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor with desired initial size, and with the specified load factor.
   *
   * @param initialCapacity desired initial size
   * @param loadFactor load factor
   */
  public Unique(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Inserts a value into the set, unless it is already present.
   * 
   * This function returns the existing value, if present.
   *
   * @param obj Object to insert or retrieve
   * @return Existing object if already present, or the new object.
   */
  @SuppressWarnings("unchecked")
  public E addOrGet(E obj) {
    int index = insertKey(obj);

    if(index < 0) {
      obj = (E) _set[-index - 1];
    }

    postInsertHook(consumeFreeSlot);
    return obj;
  }

  /**
   * Removes an existing object from the set.
   *
   * @param obj Object to remove
   * @return true if the object was found and removed.
   */
  public boolean remove(Object obj) {
    int index = index(obj);
    if(index >= 0) {
      removeAt(index);
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    super.clear();

    Arrays.fill(_set, 0, _set.length, FREE);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void rehash(int newCapacity) {
    final int oldCapacity = _set.length, oldSize = size();
    // Replace data storage:
    Object oldSet[] = _set;
    _set = new Object[newCapacity];
    Arrays.fill(_set, FREE);

    // Reinsert all objects:
    for(int i = oldCapacity; i-- > 0;) {
      E o = (E) oldSet[i];
      if(o != FREE && o != REMOVED) {
        insertKey(o);
      }
    }
    // Last check: size before and after should be the same
    reportPotentialConcurrentMod(size(), oldSize);
  }
}
