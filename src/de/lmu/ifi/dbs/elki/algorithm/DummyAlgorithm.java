package de.lmu.ifi.dbs.elki.algorithm;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Dummy Algorithm, which just iterates over all points once. Useful in testing
 * e.g. index structures.
 * 
 * @author Erich Schubert
 */
public class DummyAlgorithm<O extends DatabaseObject> extends AbstractAlgorithm<O> {

  /**
   * Empty constructor. Nothing to do.
   */
  public DummyAlgorithm() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext(); ) {
      Integer id = iter.next();
      database.get(id);
    }
  }

  public Description getDescription() {
    return new Description("Dummy","Dummy Algorithm","Iterates once over all points in the database. Useful for unit tests.","");
  }

  public Result<O> getResult() {
    return null;
  }
}
