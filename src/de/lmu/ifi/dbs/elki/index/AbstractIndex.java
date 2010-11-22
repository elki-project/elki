package de.lmu.ifi.dbs.elki.index;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public abstract class AbstractIndex<O extends DatabaseObject> implements Index<O> {
  @Override
  abstract public String getLongName();

  @Override
  abstract public String getShortName();
  
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // By default, we are not file based - no statistics available
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(O object) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(List<O> objects) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public boolean delete(O object) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }
}