package de.lmu.ifi.dbs.elki.result;


/**
 * Interface for any class that can handle results
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Result oneway - - processes
 */
public interface ResultProcessor {
  /**
   * Process a result.
   * 
   * @param baseResult The base of the result tree.
   * @param newResult Newly added result subtree.
   */
  public abstract void processNewResult(final HierarchicalResult baseResult, final Result newResult);
}
