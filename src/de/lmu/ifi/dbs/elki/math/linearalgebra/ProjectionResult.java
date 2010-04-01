package de.lmu.ifi.dbs.elki.math.linearalgebra;

/**
 * Interface representing a simple projection result.
 * 
 * This can either come from a full PCA, or just from an axis-parallel subspace selection
 * 
 * @author Erich Schubert
 */
// TODO: cleanup
public interface ProjectionResult {
  /**
   * Get the number of "strong" dimensions
   * 
   * @return number of strong (correlated) dimensions
   */
  public int getCorrelationDimension();
  
  /**
   * Projection matrix
   * 
   * @return projection matrix
   */
  public Matrix similarityMatrix();
}
