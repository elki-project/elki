/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.KNNSubList;
import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableDBIDs;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.io.FixedSizeByteBufferSerializer;

/**
 * Abstract base class for DBID factories.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.uses IntegerDBID oneway - - «create»
 * @apiviz.uses IntegerDBIDPair oneway - - «create»
 * @apiviz.uses IntegerDBIDRange oneway - - «create»
 * @apiviz.uses TroveHashSetModifiableDBIDs oneway - - «create»
 * @apiviz.uses IntegerArrayDBIDs oneway - - «create»
 */
abstract class AbstractIntegerDBIDFactory implements DBIDFactory {
  /**
   * Invalid ID.
   */
  DBID invalid = new IntegerDBID(Integer.MIN_VALUE);

  @Override
  public DBID importInteger(int id) {
    return new IntegerDBID(id);
  }

  @Override
  public void assignVar(DBIDVar var, int val) {
    if(var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(val);
    }
    else {
      var.set(new IntegerDBID(val));
    }
  }

  @Override
  public int compare(DBIDRef a, DBIDRef b) {
    final int inta = a.internalGetIndex();
    final int intb = b.internalGetIndex();
    return (inta < intb ? -1 : (inta == intb ? 0 : 1));
  }

  @Override
  public boolean equal(DBIDRef a, DBIDRef b) {
    return a.internalGetIndex() == b.internalGetIndex();
  }

  @Override
  public String toString(DBIDRef id) {
    return (id != null && id.internalGetIndex() != Integer.MIN_VALUE) //
        ? Integer.toString(id.internalGetIndex()) : "null";
  }

  @Override
  public DBIDVar newVar(DBIDRef val) {
    return new IntegerDBIDVar(val);
  }

  @Override
  public ArrayModifiableDBIDs newArray() {
    return new ArrayModifiableIntegerDBIDs();
  }

  @Override
  public HashSetModifiableDBIDs newHashSet() {
    return new FastutilIntOpenHashSetModifiableDBIDs();
  }

  @Override
  public ArrayModifiableDBIDs newArray(int size) {
    return new ArrayModifiableIntegerDBIDs(size);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(int size) {
    return new FastutilIntOpenHashSetModifiableDBIDs(size);
  }

  @Override
  public ArrayModifiableDBIDs newArray(DBIDs existing) {
    return new ArrayModifiableIntegerDBIDs(existing);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(DBIDs existing) {
    return new FastutilIntOpenHashSetModifiableDBIDs(existing);
  }

  @Override
  public DBIDPair newPair(DBIDRef first, DBIDRef second) {
    return new IntegerDBIDPair(first.internalGetIndex(), second.internalGetIndex());
  }

  @Override
  public DoubleDBIDPair newPair(double val, DBIDRef id) {
    return new DoubleIntegerDBIDPair(val, id.internalGetIndex());
  }

  @Override
  public KNNHeap newHeap(int k) {
    return new DoubleIntegerDBIDKNNHeap(k);
  }

  @Override
  public KNNHeap newHeap(KNNList exist) {
    KNNHeap heap = newHeap(exist.getK());
    // Insert backwards, as this will produce a proper heap
    for(int i = exist.size() - 1; i >= 0; i--) {
      heap.insert(exist.get(i));
    }
    return heap;
  }

  @Override
  public ModifiableDoubleDBIDList newDistanceDBIDList(int size) {
    return new DoubleIntegerDBIDList(size);
  }

  @Override
  public ModifiableDoubleDBIDList newDistanceDBIDList() {
    return new DoubleIntegerDBIDList();
  }

  @Override
  public StaticDBIDs makeUnmodifiable(DBIDs existing) {
    if(existing instanceof StaticDBIDs) {
      return (StaticDBIDs) existing;
    }
    if(existing instanceof IntegerArrayDBIDs) {
      return new UnmodifiableIntegerArrayDBIDs((IntegerArrayDBIDs) existing);
    }
    if(existing instanceof IntegerDBIDs) {
      return new UnmodifiableIntegerDBIDs((IntegerDBIDs) existing);
    }
    if(existing instanceof ArrayDBIDs) {
      return new UnmodifiableArrayDBIDs((ArrayDBIDs) existing);
    }
    return new UnmodifiableDBIDs(existing);
  }

  @Override
  public KNNList subList(KNNList list, int k) {
    if(k >= list.size()) {
      return list;
    }
    if(list instanceof IntegerDBIDKNNList) {
      return new IntegerDBIDKNNSubList((IntegerDBIDKNNList) list, k);
    }
    return new KNNSubList(list, k);
  }

  @Override
  public ByteBufferSerializer<DBID> getDBIDSerializer() {
    return IntegerDBID.DYNAMIC_SERIALIZER;
  }

  @Override
  public FixedSizeByteBufferSerializer<DBID> getDBIDSerializerStatic() {
    return IntegerDBID.STATIC_SERIALIZER;
  }

  @Override
  public Class<? extends DBID> getTypeRestriction() {
    return IntegerDBID.class;
  }

  @Override
  public DBIDRef invalid() {
    return invalid;
  }
}
