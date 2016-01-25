package de.lmu.ifi.dbs.elki.database.ids;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.ids.generic.KNNSubList;
import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.integer.IntegerArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.integer.IntegerDBIDKNNList;
import de.lmu.ifi.dbs.elki.database.ids.integer.IntegerDBIDKNNSubList;
import de.lmu.ifi.dbs.elki.database.ids.integer.IntegerDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.integer.UnmodifiableIntegerArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.integer.UnmodifiableIntegerDBIDs;
import de.lmu.ifi.dbs.elki.math.random.FastNonThreadsafeRandom;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * DBID Utility functions.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.landmark
 *
 * @apiviz.has DBIDs
 * @apiviz.has DBIDRef
 * @apiviz.composedOf DBIDFactory
 */
public final class DBIDUtil {
  /**
   * Static - no public constructor.
   */
  private DBIDUtil() {
    // Never called.
  }

  /**
   * Final, global copy of empty DBIDs.
   */
  public static final EmptyDBIDs EMPTYDBIDS = new EmptyDBIDs();

  /**
   * Get the invalid special ID.
   *
   * @return invalid ID value
   */
  public static DBIDRef invalid() {
    return DBIDFactory.FACTORY.invalid();
  }

  /**
   * Import and integer as DBID.
   *
   * Note: this may not be possible for some factories!
   *
   * @param id Integer ID to import
   * @return DBID
   */
  public static DBID importInteger(int id) {
    return DBIDFactory.FACTORY.importInteger(id);
  }

  /**
   * Export a DBID as int.
   *
   * Note: this may not be possible for some factories!
   *
   * @param id DBID to export
   * @return integer value
   */
  public static int asInteger(DBIDRef id) {
    return id.internalGetIndex();
  }

  /**
   * Compare two DBIDs.
   *
   * @param id1 First ID
   * @param id2 Second ID
   * @return Comparison result
   */
  public static int compare(DBIDRef id1, DBIDRef id2) {
    return DBIDFactory.FACTORY.compare(id1, id2);
  }

  /**
   * Test two DBIDs for equality.
   *
   * @param id1 First ID
   * @param id2 Second ID
   * @return Comparison result
   */
  public static boolean equal(DBIDRef id1, DBIDRef id2) {
    return DBIDFactory.FACTORY.equal(id1, id2);
  }

  /**
   * Dereference a DBID reference.
   *
   * @param ref DBID reference
   * @return DBID
   */
  public static DBID deref(DBIDRef ref) {
    if(ref instanceof DBID) {
      return (DBID) ref;
    }
    return importInteger(ref.internalGetIndex());
  }

  /**
   * Format a DBID as string.
   *
   * @param id DBID
   * @return String representation
   */
  public static String toString(DBIDRef id) {
    return DBIDFactory.FACTORY.toString(id);
  }

  /**
   * Format a DBID as string.
   *
   * @param ids DBIDs
   * @return String representation
   */
  public static String toString(DBIDs ids) {
    if(ids instanceof DBID) {
      return DBIDFactory.FACTORY.toString((DBID) ids);
    }
    StringBuilder buf = new StringBuilder();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(buf.length() > 0) {
        buf.append(", ");
      }
      buf.append(DBIDFactory.FACTORY.toString(iter));
    }
    return buf.toString();
  }

  /**
   * Get a serializer for DBIDs.
   *
   * @return DBID serializer
   */
  public static ByteBufferSerializer<DBID> getDBIDSerializer() {
    return DBIDFactory.FACTORY.getDBIDSerializer();
  }

  /**
   * Get a serializer for DBIDs with static size.
   *
   * @return DBID serializer
   */
  public static ByteBufferSerializer<DBID> getDBIDSerializerStatic() {
    return DBIDFactory.FACTORY.getDBIDSerializerStatic();
  }

  /**
   * Generate a single DBID.
   *
   * @return A single DBID
   */
  public static DBID generateSingleDBID() {
    return DBIDFactory.FACTORY.generateSingleDBID();
  }

  /**
   * Return a single DBID for reuse.
   *
   * @param id DBID to deallocate
   */
  public static void deallocateSingleDBID(DBID id) {
    DBIDFactory.FACTORY.deallocateSingleDBID(id);
  }

  /**
   * Generate a static DBID range.
   *
   * @param size Requested size
   * @return DBID range
   */
  public static DBIDRange generateStaticDBIDRange(int size) {
    return DBIDFactory.FACTORY.generateStaticDBIDRange(size);
  }

  /**
   * Deallocate a static DBID range.
   *
   * @param range Range to deallocate
   */
  public static void deallocateDBIDRange(DBIDRange range) {
    DBIDFactory.FACTORY.deallocateDBIDRange(range);
  }

  /**
   * Make a new DBID variable.
   *
   * @param val Initial value.
   * @return Variable
   */
  public static DBIDVar newVar(DBIDRef val) {
    return DBIDFactory.FACTORY.newVar(val);
  }

  /**
   * Make a new DBID variable.
   *
   * @return Variable
   */
  public static DBIDVar newVar() {
    return DBIDFactory.FACTORY.newVar(DBIDFactory.FACTORY.invalid());
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray() {
    return DBIDFactory.FACTORY.newArray();
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet() {
    return DBIDFactory.FACTORY.newHashSet();
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @param size Size hint
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray(int size) {
    return DBIDFactory.FACTORY.newArray(size);
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @param size Size hint
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet(int size) {
    return DBIDFactory.FACTORY.newHashSet(size);
  }

  /**
   * Make a new (modifiable) array of DBIDs.
   *
   * @param existing Existing DBIDs
   * @return New array
   */
  public static ArrayModifiableDBIDs newArray(DBIDs existing) {
    return DBIDFactory.FACTORY.newArray(existing);
  }

  /**
   * Make a new (modifiable) hash set of DBIDs.
   *
   * @param existing Existing DBIDs
   * @return New hash set
   */
  public static HashSetModifiableDBIDs newHashSet(DBIDs existing) {
    return DBIDFactory.FACTORY.newHashSet(existing);
  }

  /**
   * Compute the set intersection of two sets.
   *
   * @param first First set
   * @param second Second set
   * @return result.
   */
  // TODO: optimize better?
  public static ModifiableDBIDs intersection(DBIDs first, DBIDs second) {
    if(first.size() > second.size()) {
      return intersection(second, first);
    }
    ModifiableDBIDs inter = newHashSet(first.size());
    for(DBIDIter it = first.iter(); it.valid(); it.advance()) {
      if(second.contains(it)) {
        inter.add(it);
      }
    }
    return inter;
  }

  /**
   * Compute the set intersection size of two sets.
   *
   * @param first First set
   * @param second Second set
   * @return size
   */
  public static int intersectionSize(DBIDs first, DBIDs second) {
    // If exactly one is a Set, use it as second parameter.
    if(second instanceof SetDBIDs) {
      if(!(first instanceof SetDBIDs)) {
        return internalIntersectionSize(first, second);
      }
    }
    else {
      if(first instanceof SetDBIDs) {
        return internalIntersectionSize(second, first);
      }
    }
    // Both are the same type: both set or both non set.
    // Smaller goes first.
    if(first.size() <= second.size()) {
      return internalIntersectionSize(first, second);
    }
    else {
      return internalIntersectionSize(second, first);
    }
  }

  /**
   * Compute the set intersection size of two sets.
   *
   * @param first First set
   * @param second Second set
   * @return size
   */
  private static int internalIntersectionSize(DBIDs first, DBIDs second) {
    int c = 0;
    for(DBIDIter it = first.iter(); it.valid(); it.advance()) {
      if(second.contains(it)) {
        c++;
      }
    }
    return c;
  }

  /**
   * Compute the set symmetric intersection of two sets.
   *
   * @param first First set
   * @param second Second set
   * @param firstonly OUTPUT: elements only in first. MUST BE EMPTY
   * @param intersection OUTPUT: elements in intersection. MUST BE EMPTY
   * @param secondonly OUTPUT: elements only in second. MUST BE EMPTY
   */
  // TODO: optimize?
  public static void symmetricIntersection(DBIDs first, DBIDs second, HashSetModifiableDBIDs firstonly, HashSetModifiableDBIDs intersection, HashSetModifiableDBIDs secondonly) {
    if(first.size() > second.size()) {
      symmetricIntersection(second, first, secondonly, intersection, firstonly);
      return;
    }
    assert (firstonly.size() == 0) : "OUTPUT set should be empty!";
    assert (intersection.size() == 0) : "OUTPUT set should be empty!";
    assert (secondonly.size() == 0) : "OUTPUT set should be empty!";
    // Initialize with second
    secondonly.addDBIDs(second);
    for(DBIDIter it = first.iter(); it.valid(); it.advance()) {
      // Try to remove
      if(secondonly.remove(it)) {
        intersection.add(it);
      }
      else {
        firstonly.add(it);
      }
    }
  }

  /**
   * Returns the union of the two specified collection of IDs.
   *
   * @param ids1 the first collection
   * @param ids2 the second collection
   * @return the union of ids1 and ids2 without duplicates
   */
  public static ModifiableDBIDs union(DBIDs ids1, DBIDs ids2) {
    ModifiableDBIDs result = DBIDUtil.newHashSet(Math.max(ids1.size(), ids2.size()));
    result.addDBIDs(ids1);
    result.addDBIDs(ids2);
    return result;
  }

  /**
   * Returns the difference of the two specified collection of IDs.
   *
   * @param ids1 the first collection
   * @param ids2 the second collection
   * @return the difference of ids1 minus ids2
   */
  public static ModifiableDBIDs difference(DBIDs ids1, DBIDs ids2) {
    ModifiableDBIDs result = DBIDUtil.newHashSet(ids1);
    result.removeDBIDs(ids2);
    return result;
  }

  /**
   * Wrap an existing DBIDs collection to be unmodifiable.
   *
   * @param existing Existing collection
   * @return Unmodifiable collection
   */
  public static StaticDBIDs makeUnmodifiable(DBIDs existing) {
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

  /**
   * Ensure that the given DBIDs are array-indexable.
   *
   * @param ids IDs
   * @return Array DBIDs.
   */
  public static ArrayDBIDs ensureArray(DBIDs ids) {
    if(ids instanceof ArrayDBIDs) {
      return (ArrayDBIDs) ids;
    }
    else {
      return newArray(ids);
    }
  }

  /**
   * Ensure that the given DBIDs support fast "contains" operations.
   *
   * @param ids IDs
   * @return Set DBIDs.
   */
  public static SetDBIDs ensureSet(DBIDs ids) {
    if(ids instanceof SetDBIDs) {
      return (SetDBIDs) ids;
    }
    else {
      return newHashSet(ids);
    }
  }

  /**
   * Ensure modifiable.
   *
   * @param ids IDs
   * @return Modifiable DBIDs.
   */
  public static ModifiableDBIDs ensureModifiable(DBIDs ids) {
    if(ids instanceof ModifiableDBIDs) {
      return (ModifiableDBIDs) ids;
    }
    else {
      if(ids instanceof ArrayDBIDs) {
        return newArray(ids);
      }
      if(ids instanceof HashSetDBIDs) {
        return newHashSet(ids);
      }
      return newArray(ids);
    }
  }

  /**
   * Make a DBID pair.
   *
   * @param id1 first ID
   * @param id2 second ID
   *
   * @return DBID pair
   */
  public static DBIDPair newPair(DBIDRef id1, DBIDRef id2) {
    return DBIDFactory.FACTORY.newPair(id1, id2);
  }

  /**
   * Make a DoubleDBIDPair.
   *
   * @param val double value
   * @param id ID
   * @return new pair
   */
  public static DoubleDBIDPair newPair(double val, DBIDRef id) {
    return DBIDFactory.FACTORY.newPair(val, id);
  }

  /**
   * Create an appropriate heap for the distance type.
   *
   * This will use a double heap if appropriate.
   *
   * @param k K value
   * @return New heap of size k, appropriate for this distance type.
   */
  public static KNNHeap newHeap(int k) {
    return DBIDFactory.FACTORY.newHeap(k);
  }

  /**
   * Build a new heap from a given list.
   *
   * @param exist Existing result
   * @return New heap
   */
  public static KNNHeap newHeap(KNNList exist) {
    return DBIDFactory.FACTORY.newHeap(exist);
  }

  /**
   * Produce a random shuffling of the given DBID array.
   *
   * @param ids Original DBIDs
   * @param rnd Random generator
   */
  public static void randomShuffle(ArrayModifiableDBIDs ids, RandomFactory rnd) {
    randomShuffle(ids, rnd.getSingleThreadedRandom(), ids.size());
  }

  /**
   * Produce a random shuffling of the given DBID array.
   *
   * @param ids Original DBIDs
   * @param random Random generator
   */
  public static void randomShuffle(ArrayModifiableDBIDs ids, Random random) {
    randomShuffle(ids, random, ids.size());
  }

  /**
   * Produce a random shuffling of the given DBID array.
   *
   * Only the first {@code limit} elements will be fully randomized, but the
   * remaining objects will also be changed.
   *
   * @param ids Original DBIDs
   * @param random Random generator
   * @param limit Shuffling limit.
   */
  public static void randomShuffle(ArrayModifiableDBIDs ids, Random random, final int limit) {
    final int end = ids.size();
    for(int i = 1; i < limit; i++) {
      ids.swap(i - 1, i + random.nextInt(end - i));
    }
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param source Original DBIDs
   * @param k k Parameter
   * @param seed Random generator seed
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, int seed) {
    return randomSample(source, k, new Random(seed));
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param source Original DBIDs
   * @param k k Parameter
   * @param seed Random generator seed
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, Long seed) {
    if(seed != null) {
      return randomSample(source, k, new Random(seed.longValue()));
    }
    else {
      return randomSample(source, k, new Random());
    }
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param source Original DBIDs
   * @param k k Parameter
   * @param rnd Random generator
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, RandomFactory rnd) {
    return randomSample(source, k, rnd.getSingleThreadedRandom());
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param source Original DBIDs
   * @param k k Parameter
   * @param random Random generator
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, Random random) {
    if(k < 0 || k > source.size()) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k + " > " + source.size() + " or < 0");
    }
    if(random == null) {
      // Fast, and we're single-threaded here anyway.
      random = new FastNonThreadsafeRandom();
    }

    // TODO: better balancing for different sizes
    // Two methods: constructive vs. destructive
    if(k < source.size() >> 1) {
      ArrayDBIDs aids = DBIDUtil.ensureArray(source);
      DBIDArrayIter iter = aids.iter();
      HashSetModifiableDBIDs sample = DBIDUtil.newHashSet(k);
      while(sample.size() < k) {
        iter.seek(random.nextInt(aids.size()));
        sample.add(iter);
      }
      return sample;
    }
    else {
      ArrayModifiableDBIDs sample = DBIDUtil.newArray(source);
      randomShuffle(sample, random, k);
      // Delete trailing elements
      for(int i = sample.size() - 1; i >= k; i--) {
        sample.remove(i);
      }
      return sample;
    }
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param ids Original ids
   * @param rate Sampling rate
   * @param random Random generator
   * @return Sample
   */
  public static DBIDs randomSample(DBIDs ids, double rate, RandomFactory random) {
    return randomSample(ids, rate, random.getSingleThreadedRandom());
  }

  /**
   * Produce a random sample of the given DBIDs.
   *
   * @param ids Original ids
   * @param rate Sampling rate
   * @param random Random generator
   * @return Sample
   */
  public static DBIDs randomSample(DBIDs ids, double rate, Random random) {
    if(rate <= 0) {
      return ids;
    }
    if(rate < 1.1) {
      int size = Math.min((int) (rate * ids.size()), ids.size());
      return randomSample(ids, size, random);
    }
    int size = Math.min((int) rate, ids.size());
    return randomSample(ids, size, random);
  }

  /**
   * Draw a single random sample.
   *
   * @param ids IDs to draw from
   * @param random Random value
   * @return Random ID
   */
  public static DBIDVar randomSample(DBIDs ids, Random random) {
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    DBIDVar v = DBIDUtil.newVar();
    aids.assignVar(random.nextInt(aids.size()), v);
    return v;
  }

  /**
   * Draw a single random sample.
   *
   * @param ids IDs to draw from
   * @param random Random value
   * @return Random ID
   */
  public static DBIDVar randomSample(DBIDs ids, RandomFactory random) {
    return randomSample(ids, random.getSingleThreadedRandom());
  }

  /**
   * Randomly split IDs into {@code p} partitions of almost-equal size.
   *
   * @param ids Original DBIDs
   * @param p Desired number of partitions.
   * @param rnd Random generator
   */
  public static ArrayDBIDs[] randomSplit(DBIDs ids, int p, RandomFactory rnd) {
    return randomSplit(ids, p, rnd.getSingleThreadedRandom());
  }

  /**
   * Randomly split IDs into {@code p} partitions of almost-equal size.
   *
   * @param oids Original DBIDs
   * @param p Desired number of partitions.
   * @param random Random generator
   */
  public static ArrayDBIDs[] randomSplit(DBIDs oids, int p, Random random) {
    if(random == null) {
      // Fast, and we're single-threaded here anyway.
      random = new FastNonThreadsafeRandom();
    }
    ArrayModifiableDBIDs ids = newArray(oids);
    final int size = ids.size();
    ArrayDBIDs[] split = new ArrayDBIDs[p];
    // Shuffle
    for(int i = 1; i < size; i++) {
      ids.swap(i - 1, i + random.nextInt(size - i));
    }
    final int minsize = size / p; // Floor.
    final int extra = size % p; // Remainder
    for(int beg = 0, part = 0; part < p; part++) {
      // First partitions are smaller, last partitions are larger.
      final int psize = minsize + ((part < extra) ? 1 : 0);
      split[part] = ids.slice(beg, beg + psize);
      beg += psize;
    }
    return split;
  }

  /**
   * Get a subset of the KNN result.
   *
   * @param list Existing list
   * @param k k
   * @return Subset
   */
  public static KNNList subList(KNNList list, int k) {
    if(k >= list.size()) {
      return list;
    }
    if(list instanceof IntegerDBIDKNNList) {
      return new IntegerDBIDKNNSubList((IntegerDBIDKNNList) list, k);
    }
    return new KNNSubList(list, k);
  }

  /**
   * Create a modifiable list to store distance-DBID pairs.
   *
   * @param size Estimated upper list size
   * @return Empty list
   */
  public static ModifiableDoubleDBIDList newDistanceDBIDList(int size) {
    return DBIDFactory.FACTORY.newDistanceDBIDList(size);
  }

  /**
   * Create a modifiable list to store distance-DBID pairs.
   *
   * @return Empty list
   */
  public static ModifiableDoubleDBIDList newDistanceDBIDList() {
    return DBIDFactory.FACTORY.newDistanceDBIDList();
  }

  /**
   * Assert that the presented ids constitute a continuous {@link DBIDRange}.
   *
   * @param ids ID range.
   * @return DBID range.
   * @throws AbortException
   */
  public static DBIDRange assertRange(DBIDs ids) {
    if(!(ids instanceof DBIDRange)) {
      throw new AbortException("This class may currently only be used with static databases and DBID ranges.");
    }
    return (DBIDRange) ids;
  }
}
