package de.lmu.ifi.dbs.elki.result;

/**
 * Result with an internal hierarchy.
 * 
 * Note: while this often seems a bit clumsy to use, the benefit of having this
 * delegate is to keep the APIs simpler, and thus make ELKI development easier.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ResultHierarchy
 */
public interface HierarchicalResult extends Result {
  /**
   * Get the objects current hierarchy - may be {@code null}!
   * 
   * @return current hierarchy. May be {@code null}!
   */
  public ResultHierarchy getHierarchy();

  /**
   * Set (exchange) the hierarchy implementation (e.g. after merging!)
   * 
   * @param hierarchy New hierarchy
   */
  public void setHierarchy(ResultHierarchy hierarchy);
}
