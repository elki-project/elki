package experimentalcode.erich.newdblayer.ids.integer;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.Logging;
import experimentalcode.erich.newdblayer.ids.ArrayModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDFactory;
import experimentalcode.erich.newdblayer.ids.DBIDs;
import experimentalcode.erich.newdblayer.ids.HashSetModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.StaticArrayDBIDs;
import experimentalcode.erich.newdblayer.ids.TreeSetModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.generic.GenericArrayModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.generic.GenericHashSetModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.generic.GenericTreeSetModifiableDBIDs;

/**
 * Simple DBID management, that never reuses IDs.
 * Statically allocated DBID ranges are given positive values,
 * Dynamically allocated DBIDs are given negative values.
 * 
 * @author Erich Schubert
 */
public class SimpleDBIDFactory implements DBIDFactory {
  /**
   * Logging for error messages.
   */
  Logging logger = Logging.getLogger(SimpleDBIDFactory.class);
  
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
  public synchronized StaticArrayDBIDs generateStaticDBIDRange(int size) {
    if (rangestart >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    StaticArrayDBIDs alloc = new DBIDRange(rangestart, size);
    rangestart += size;
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(@SuppressWarnings("unused") StaticArrayDBIDs range) {
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