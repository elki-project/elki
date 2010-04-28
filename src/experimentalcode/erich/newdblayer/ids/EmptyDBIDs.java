package experimentalcode.erich.newdblayer.ids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.EmptyIterator;

/**
 * Empty DBID collection.
 * 
 * @author Erich Schubert
 *
 */
public class EmptyDBIDs implements DBIDs {
  @Override
  public Collection<DBID> asCollection() {
    return new ArrayList<DBID>(0);
  }

  @Override
  public boolean contains(@SuppressWarnings("unused") Object o) {
    return false;
  }

  @Override
  public Iterator<DBID> iterator() {
    return EmptyIterator.STATIC();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }
}
