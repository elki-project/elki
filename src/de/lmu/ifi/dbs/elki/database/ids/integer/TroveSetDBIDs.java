package de.lmu.ifi.dbs.elki.database.ids.integer;
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

import gnu.trove.set.TIntSet;

import java.util.AbstractSet;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;

/**
 * Abstract base class for GNU Trove array based lists.
 * 
 * @author Erich Schubert
 */
public abstract class TroveSetDBIDs extends AbstractSet<DBID> implements SetDBIDs {
  /**
   * Get the array store
   * 
   * @return the store
   */
  abstract protected TIntSet getStore();

  @Override
  public Iterator<DBID> iterator() {
    return new TroveIteratorAdapter(getStore().iterator());
  }

  @Override
  public int size() {
    return getStore().size();
  }

  @Override
  public boolean contains(Object o) {
    if(o instanceof DBID) {
      return getStore().contains(((DBID) o).getIntegerID());
    }
    return false;
  }
}