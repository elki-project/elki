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
package de.lmu.ifi.dbs.elki.database.ids;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.io.FixedSizeByteBufferSerializer;

/**
 * Factory interface for generating DBIDs. See {@link #FACTORY} for the static
 * instance to use.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - DBID
 * @navassoc - create - DBIDs
 * @navassoc - create - DBIDPair
 * @navassoc - create - DBIDRange
 * @navassoc - create - ArrayModifiableDBIDs
 * @navassoc - create - HashSetModifiableDBIDs
 * @navassoc - create - HashSetModifiableDBIDs
 * @navhas - provides - ByteBufferSerializer
 */
public interface DBIDFactory {
  /**
   * Static DBID factory to use.
   */
  DBIDFactory FACTORY = ClassGenericsUtil.instantiateLowlevel(DBIDFactory.class);

  /**
   * Make a new DBID variable.
   *
   * @param val Initial value.
   * @return Variable
   */
  DBIDVar newVar(DBIDRef val);

  /**
   * Import and integer as DBID.
   *
   * Note: this may not be possible for some factories!
   *
   * @param id Integer ID to import
   * @return DBID
   */
  DBID importInteger(int id);

  /**
   * Assign an integer value to a DBID variable.
   * <p>
   * Note: this may not be possible for some factories!
   *
   * @param var Variable
   * @param val Integer value
   */
  DBIDVar assignVar(DBIDVar var, int val);

  /**
   * Generate a single DBID.
   *
   * @return A single DBID
   */
  DBID generateSingleDBID();

  /**
   * Return a single DBID for reuse.
   *
   * @param id DBID to deallocate
   */
  void deallocateSingleDBID(DBIDRef id);

  /**
   * Generate a static DBID range.
   *
   * @param size Requested size
   * @return DBID range
   */
  DBIDRange generateStaticDBIDRange(int size);

  /**
   * Generate a static DBID range.
   *
   * @param begin Range begin
   * @param size Requested size
   * @return DBID range
   */
  DBIDRange generateStaticDBIDRange(int begin, int size);

  /**
   * Deallocate a static DBID range.
   *
   * @param range Range to deallocate
   */
  void deallocateDBIDRange(DBIDRange range);

  /**
   * Make a DBID pair from two existing DBIDs.
   *
   * @param id1 first DBID
   * @param id2 second DBID
   *
   * @return new pair.
   */
  DBIDPair newPair(DBIDRef id1, DBIDRef id2);

  /**
   * Make a double-DBID pair.
   *
   * @param val Double value
   * @param id DBID
   * @return New pair
   */
  DoubleDBIDPair newPair(double val, DBIDRef id);

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @return New array
   */
  ArrayModifiableDBIDs newArray();

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @return New hash set
   */
  HashSetModifiableDBIDs newHashSet();

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @param size Size hint
   * @return New array
   */
  ArrayModifiableDBIDs newArray(int size);

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @param size Size hint
   * @return New hash set
   */
  HashSetModifiableDBIDs newHashSet(int size);

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @param existing existing DBIDs to use
   * @return New array
   */
  ArrayModifiableDBIDs newArray(DBIDs existing);

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @param existing existing DBIDs to use
   * @return New hash set
   */
  HashSetModifiableDBIDs newHashSet(DBIDs existing);

  /**
   * Create an heap for kNN search.
   *
   * @param k K value
   * @return New heap of size k.
   */
  KNNHeap newHeap(int k);

  /**
   * Build a new heap from a given list.
   *
   * @param exist Existing result
   * @return New heap
   */
  KNNHeap newHeap(KNNList exist);

  /**
   * Get a serializer for DBIDs.
   *
   * @return DBID serializer
   */
  ByteBufferSerializer<DBID> getDBIDSerializer();

  /**
   * Get a serializer for DBIDs with static size.
   *
   * @return DBID serializer
   */
  FixedSizeByteBufferSerializer<DBID> getDBIDSerializerStatic();

  /**
   * Get type restriction.
   *
   * @return type restriction for DBIDs
   */
  Class<? extends DBID> getTypeRestriction();

  /**
   * Compare two DBIDs, for sorting.
   *
   * @param a First
   * @param b Second
   * @return Comparison result
   */
  int compare(DBIDRef a, DBIDRef b);

  /**
   * Compare two DBIDs, for equality testing.
   *
   * @param a First
   * @param b Second
   * @return Comparison result
   */
  boolean equal(DBIDRef a, DBIDRef b);

  /**
   * Print a DBID as string.
   *
   * @param id DBID reference
   * @return Formatted ID
   */
  String toString(DBIDRef id);

  /**
   * Get the invalid DBID value, usable as "undefined" placeholder.
   *
   * @return Invalid value
   */
  DBIDRef invalid();

  /**
   * Create a modifiable list to store distance-DBID pairs.
   *
   * @param size initial size estimate
   * @return New list of given initial size
   */
  ModifiableDoubleDBIDList newDistanceDBIDList(int size);

  /**
   * Create a modifiable list to store distance-DBID pairs.
   *
   * @return New list
   */
  ModifiableDoubleDBIDList newDistanceDBIDList();

  /**
   * Make DBIDs immutable.
   *
   * @param existing Existing DBIDs
   * @return Immutable version
   */
  StaticDBIDs makeUnmodifiable(DBIDs existing);
}
