package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.RangeDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.GenericArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.GenericHashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.GenericTreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Simple DBID management, that never reuses IDs.
 * Statically allocated DBID ranges are given positive values,
 * Dynamically allocated DBIDs are given negative values.
 * 
 * @author Erich Schubert
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
    if (dynamicids == Integer.MIN_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    dynamicids--;
    return new IntegerDBID(dynamicids);
  }

  @Override
  public void deallocateSingleDBID(@SuppressWarnings("unused") DBID id) {
    // ignore.
  }

  @Override
  public synchronized RangeDBIDs generateStaticDBIDRange(int size) {
    if (rangestart >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    RangeDBIDs alloc = new IntegerDBIDRange(rangestart, size);
    rangestart += size;
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(@SuppressWarnings("unused") RangeDBIDs range) {
    // ignore.
  }

  @Override
  public DBID importInteger(int id) {
    return new IntegerDBID(id);
  }

  @Override
  public ArrayModifiableDBIDs newArray() {
    return new GenericArrayModifiableDBIDs();
  }

  @Override
  public HashSetModifiableDBIDs newHashSet() {
    return new GenericHashSetModifiableDBIDs();
  }

  @Override
  public TreeSetModifiableDBIDs newTreeSet() {
    return new GenericTreeSetModifiableDBIDs();
  }

  @Override
  public ArrayModifiableDBIDs newArray(int size) {
    return new GenericArrayModifiableDBIDs(size);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(int size) {
    return new GenericHashSetModifiableDBIDs(size);
  }

  @Override
  public TreeSetModifiableDBIDs newTreeSet(int size) {
    return new GenericTreeSetModifiableDBIDs(size);
  }

  @Override
  public ArrayModifiableDBIDs newArray(DBIDs existing) {
    return new GenericArrayModifiableDBIDs(existing);
  }

  @Override
  public HashSetModifiableDBIDs newHashSet(DBIDs existing) {
    return new GenericHashSetModifiableDBIDs(existing);
  }

  @Override
  public TreeSetModifiableDBIDs newTreeSet(DBIDs existing) {
    return new GenericTreeSetModifiableDBIDs(existing);
  }
}