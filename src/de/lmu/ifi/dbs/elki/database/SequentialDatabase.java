package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SequentialDatabase is a simple implementation of a Database.
 * <p/>
 * It does not support any index structure and holds all objects in main memory
 * (as a Map).
 * 
 * @author Arthur Zimek
 * @param <O> the type of FeatureVector as element of the database
 */
@Description("Database using an in-memory hashtable and doing linear scans.")
public class SequentialDatabase<O extends DatabaseObject> extends AbstractDatabase<O> implements Parameterizable {
  /**
   * Provides a database for main memory holding all objects in a hashtable.
   */
  public SequentialDatabase() {
    super();
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>();
  }
}
