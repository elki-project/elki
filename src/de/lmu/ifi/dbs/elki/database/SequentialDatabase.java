package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;

/**
 * SequentialDatabase is a simple implementation of a Database.
 * <p/>
 * It does not support any index structure and holds all objects in main memory
 * (as a Map).
 * 
 * @author Arthur Zimek
 * @param <O> the type of FeatureVector as element of the database
 * 
 * @deprecated Use the HashmapDatabase instead
 */
@Deprecated
public class SequentialDatabase<O extends DatabaseObject> extends HashmapDatabase<O> implements Parameterizable {
  /**
   * Provides a database for main memory holding all objects in a hashtable.
   */
  public SequentialDatabase() {
    super(new EmptyParameterization());
  }
}
