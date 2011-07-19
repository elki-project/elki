package de.lmu.ifi.dbs.elki.result;


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

  @SuppressWarnings("unused")
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // always ignore the new result.
  }
}