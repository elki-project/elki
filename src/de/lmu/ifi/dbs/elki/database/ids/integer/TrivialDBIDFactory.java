package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
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
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Trivial DBID management, that never reuses IDs and just gives them out in sequence.
 * Statically allocated DBID ranges are given positive values,
 * Dynamically allocated DBIDs are given negative values.
 * 
 * @author Erich Schubert
 */
public class TrivialDBIDFactory implements DBIDFactory {
  /**
   * Logging for error messages.
   */
  Logging logger = Logging.getLogger(TrivialDBIDFactory.class);
  
  /**
   * Keep track of the smallest dynamic DBID offset not used
   */
  int next = 1;
  
  /**
   * Constructor
   */
  public TrivialDBIDFactory() {
    super();
  }

  @Override
  public synchronized DBID generateSingleDBID() {
    if (next == Integer.MAX_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBID ret = new IntegerDBID(next);
    next++;
    return ret;
  }

  @Override
  public void deallocateSingleDBID(@SuppressWarnings("unused") DBID id) {
    // ignore.
  }

  @Override
  public synchronized RangeDBIDs generateStaticDBIDRange(int size) {
    if (next >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    RangeDBIDs alloc = new IntegerDBIDRange(next, size);
    next += size;
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