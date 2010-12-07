package de.lmu.ifi.dbs.elki.result;


/**
 * Abstract class for a result object with hierarchy
 * 
 * @author Erich Schubert
 */
public abstract class AbstractHierarchicalResult implements HierarchicalResult {
  /**
   * The hierarchy storage.
   */
  private ResultHierarchy hierarchy;
  
  /**
   * Constructor.
   */
  public AbstractHierarchicalResult() {
    super();
    this.hierarchy = new ResultHierarchy();
  }

  @Override
  public final ResultHierarchy getHierarchy() {
    return hierarchy;
  }

  @Override
  public final void setHierarchy(ResultHierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }
  
  /**
   * Add a child result.
   * 
   * @param child Child result
   */
  public void addChildResult(Result child) {
    hierarchy.add(this, child);
  }
}