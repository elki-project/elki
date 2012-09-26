package de.lmu.ifi.dbs.elki.database.ids.integer;

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

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.persistent.FixedSizeByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Simple DBID management, that never reuses IDs. Statically allocated DBID
 * ranges are given positive values, Dynamically allocated DBIDs are given
 * negative values.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.uses IntegerDBID oneway - - «create»
 * @apiviz.uses IntegerDBIDPair oneway - - «create»
 * @apiviz.uses IntegerDBIDRange oneway - - «create»
 * @apiviz.uses TroveArrayModifiableDBIDs oneway - - «create»
 * @apiviz.uses TroveHashSetModifiableDBIDs oneway - - «create»
 */
public class SimpleDBIDFactory implements DBIDFactory {
  /**
   * Keep track of the smallest dynamic DBID offset not used.
   */
  int dynamicids = 0;

  /**
   * The starting point for static DBID range allocations.
   */
  int rangestart = 0;

  /**
   * Constructor.
   */
  public SimpleDBIDFactory() {
    super();
  }

  @Override
  public synchronized DBID generateSingleDBID() {
    if(dynamicids == Integer.MIN_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    dynamicids--;
    return new IntegerDBID(dynamicids);
  }

  @Override
  public void deallocateSingleDBID(DBID id) {
    // ignore.
  }

  @Override
  public synchronized DBIDRange generateStaticDBIDRange(int size) {
    if(rangestart >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRange alloc = new IntegerDBIDRange(rangestart, size);
    rangestart += size;
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(DBIDRange range) {
    // ignore.
  }

  @Override
  public DBID importInteger(int id) {
    return new IntegerDBID(id);
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
    return Integer.toString(id.internalGetIndex());
  }

  @Override
  public ArrayModifiableDBIDs newArray() {
    return new TroveArrayModifiableDBIDs();
  }

  @Override
  public HashSetModifiableDBIDs newHashSet() {
    return new TroveHashSetModifiableDBIDs();
  }

  @Override
  public ArrayModifiableDBIDs newArray(int size) {
    return new TroveArrayModifiableDBIDs(size);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(int size) {
    return new TroveHashSetModifiableDBIDs(size);
  }

  @Override
  public ArrayModifiableDBIDs newArray(DBIDs existing) {
    return new TroveArrayModifiableDBIDs(existing);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(DBIDs existing) {
    return new TroveHashSetModifiableDBIDs(existing);
  }

  @Override
  public DBIDPair newPair(DBIDRef first, DBIDRef second) {
    return new IntegerDBIDPair(first.internalGetIndex(), second.internalGetIndex());
  }

  @Override
  public DoubleDBIDPair newPair(double val, DBIDRef id) {
    return new IntegerDoubleDBIDPair(val, id.internalGetIndex());
  }

  @Override
  public <D extends Distance<D>> DistanceDBIDPair<D> newDistancePair(D val, DBIDRef id) {
    if (val instanceof DoubleDistance) {
      return (DistanceDBIDPair<D>) new DoubleDistanceIntegerDBIDPair(((DoubleDistance) val).doubleValue(), id.internalGetIndex());
    }
    return new DistanceIntegerDBIDPair<D>(val, id.internalGetIndex());
  }

  @Override
  public DoubleDistanceDBIDPair newDistancePair(double val, DBIDRef id) {
    return new DoubleDistanceIntegerDBIDPair(val, id.internalGetIndex());
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
}