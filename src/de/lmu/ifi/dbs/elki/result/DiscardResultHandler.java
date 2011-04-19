package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.Database;

/**
 * A dummy result handler that discards the actual result, for use in
 * benchmarks.
 * 
 * @author Erich Schubert
 */
public class DiscardResultHandler implements ResultHandler {
  /**
   * Default constructor.
   */
  public DiscardResultHandler() {
    // empty constructor
  }

  /**
   * Process the result... by discarding
   * 
   * @param db discarded
   * @param result discarded
   */
  @Override
  public void processResult(Database db, Result result) {
    // discard the result.
  }
}