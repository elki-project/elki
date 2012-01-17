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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
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
   * Keep track of the smallest dynamic DBID offset not used
   */
  int dynamicids = 0;

  /**
   * The starting point for static DBID range allocations.
   */
  int rangestart = 0;

  /**
   * Constructor
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
  public DBIDPair makePair(DBID first, DBID second) {
    return new IntegerDBIDPair(first.getIntegerID(), second.getIntegerID());
  }

  @Override
  public ByteBufferSerializer<DBID> getDBIDSerializer() {
    return IntegerDBID.dynamicSerializer;
  }

  @Override
  public FixedSizeByteBufferSerializer<DBID> getDBIDSerializerStatic() {
    return IntegerDBID.staticSerializer;
  }

  @Override
  public Class<? extends DBID> getTypeRestriction() {
    return IntegerDBID.class;
  }
}