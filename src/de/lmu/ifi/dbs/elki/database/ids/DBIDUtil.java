package de.lmu.ifi.dbs.elki.database.ids;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableDBIDs;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * DBID Utility functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.ids.DBIDFactory
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
   * Import an Integer DBID.
   * 
   * @param id Integer ID
   * @return DBID
   */
  public static DBID importInteger(int id) {
    return DBIDFactory.FACTORY.importInteger(id);
  }

  /**
   * Get a serializer for DBIDs
   * 
   * @return DBID serializer
   */
  public ByteBufferSerializer<DBID> getDBIDSerializer() {
    return DBIDFactory.FACTORY.getDBIDSerializer();
  }

  /**
   * Get a serializer for DBIDs with static size
   * 
   * @return DBID serializer
   */
  public ByteBufferSerializer<DBID> getDBIDSerializerStatic() {
    return DBIDFactory.FACTORY.getDBIDSerializerStatic();
  }

  /**
   * Generate a single DBID
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
  // TODO: optimize?
  public static ModifiableDBIDs intersection(DBIDs first, DBIDs second) {
    if(first.size() > second.size()) {
      return intersection(second, first);
    }
    ModifiableDBIDs inter = newHashSet(first.size());
    for(DBIDIter it = first.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      if(second.contains(id)) {
        inter.add(id);
      }
    }
    return inter;
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
    assert(firstonly.size() == 0) : "OUTPUT set should be empty!";
    assert(intersection.size() == 0) : "OUTPUT set should be empty!";
    assert(secondonly.size() == 0) : "OUTPUT set should be empty!";
    // Initialize with second
    secondonly.addDBIDs(second);
    for(DBIDIter it = first.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      // Try to remove
      if(secondonly.remove(id)) {
        intersection.add(id);
      } else {
        firstonly.add(id);
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
    ModifiableDBIDs result = DBIDUtil.newHashSet();
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
    ModifiableDBIDs result = DBIDUtil.newHashSet();
    result.addDBIDs(ids1);
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
    if (existing instanceof ArrayDBIDs) {
      return new UnmodifiableArrayDBIDs((ArrayDBIDs)existing);
    } else {
      return new UnmodifiableDBIDs(existing);
    }
  }

  /**
   * Ensure that the given DBIDs are array-indexable.
   * 
   * @param ids
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
   * @param ids
   * @return Array DBIDs.
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
   * Ensure modifiable
   * 
   * @param ids
   * @return Array DBIDs.
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
  public static DBIDPair newPair(DBID id1, DBID id2) {
    return DBIDFactory.FACTORY.makePair(id1, id2);
  }

  /**
   * Produce a random sample of the given DBIDs
   * 
   * @param source Original DBIDs
   * @param k k Parameter
   * @param seed Random generator seed
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, int seed) {
    return randomSample(source, k, (long) seed);
  }

  /**
   * Produce a random sample of the given DBIDs
   * 
   * @param source Original DBIDs
   * @param k k Parameter
   * @param seed Random generator seed
   * @return new DBIDs
   */
  public static ModifiableDBIDs randomSample(DBIDs source, int k, Long seed) {
    if(k <= 0 || k > source.size()) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k+ " > "+source.size()+" or < 0");
    }
    final Random random;
    if(seed != null) {
      random = new Random(seed);
    }
    else {
      random = new Random();
    }
    // TODO: better balancing for different sizes
    // Two methods: constructive vs. destructive
    if(k < source.size() / 2) {
      ArrayDBIDs aids = DBIDUtil.ensureArray(source);
      HashSetModifiableDBIDs sample = DBIDUtil.newHashSet(k);
      while(sample.size() < k) {
        sample.add(aids.get(random.nextInt(aids.size())));
      }
      return sample;
    }
    else {
      ArrayModifiableDBIDs sample = DBIDUtil.newArray(source);
      while(sample.size() > k) {
        // Element to remove
        int idx = random.nextInt(sample.size());
        // Remove last element
        DBID last = sample.remove(sample.size() - 1);
        // Replace target element:
        if(idx < sample.size()) {
          sample.set(idx, last);
        }
      }
      return sample;
    }
  }
}