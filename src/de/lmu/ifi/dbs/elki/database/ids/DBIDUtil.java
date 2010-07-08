package de.lmu.ifi.dbs.elki.database.ids;

import de.lmu.ifi.dbs.elki.database.ids.generic.UnmodifiableDBIDs;

/**
 * DBID Utility functions.
 * 
 * @author Erich Schubert
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
  public static RangeDBIDs generateStaticDBIDRange(int size) {
    return DBIDFactory.FACTORY.generateStaticDBIDRange(size);
  }

  /**
   * Deallocate a static DBID range.
   * 
   * @param range Range to deallocate
   */
  public static void deallocateDBIDRange(RangeDBIDs range) {
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
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet() {
    return DBIDFactory.FACTORY.newTreeSet();
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
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @param size Size hint
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet(int size) {
    return DBIDFactory.FACTORY.newTreeSet(size);
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
   * Make a new (modifiable) tree set of DBIDs.
   * 
   * @param existing Existing DBIDs
   * @return New tree set
   */
  public static TreeSetModifiableDBIDs newTreeSet(DBIDs existing) {
    return DBIDFactory.FACTORY.newTreeSet(existing);
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
    for(DBID id : first) {
      if(second.contains(id)) {
        inter.add(id);
      }
    }
    return inter;
  }

  /**
   * Wrap an existing DBIDs collection to be unmodifiable.
   * 
   * @param existing Existing collection
   * @return Unmodifiable collection
   */
  public static DBIDs makeUnmodifiable(DBIDs existing) {
    if(existing instanceof StaticDBIDs) {
      return existing;
    }
    if(existing instanceof UnmodifiableDBIDs) {
      return existing;
    }
    return new UnmodifiableDBIDs(existing);
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
    if(ids instanceof HashSetDBIDs) {
      return (HashSetDBIDs) ids;
    }
    else if(ids instanceof TreeSetDBIDs) {
      return (TreeSetDBIDs) ids;
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
      if (ids instanceof ArrayDBIDs) {
        return newArray(ids);
      }
      if (ids instanceof HashSetDBIDs) {
        return newHashSet(ids);
      }
      if (ids instanceof TreeSetDBIDs) {
        return newTreeSet(ids);
      }
      return newArray(ids);
    }
  }
}