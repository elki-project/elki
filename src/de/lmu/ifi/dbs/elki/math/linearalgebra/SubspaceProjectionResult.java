package de.lmu.ifi.dbs.elki.math.linearalgebra;

/**
 * Simple class wrapping the result of a subspace projection.
 * 
 * @author Erich Schubert
 */
public class SubspaceProjectionResult implements ProjectionResult {
  /**
   * The correlation dimensionality
   */
  private int correlationDimensionality;
  
  /**
   * The similarity matrix
   */
  private Matrix similarityMat;
  
  /**
   * Constructor.
   * 
   * @param correlationDimensionality dimensionality
   * @param similarityMat projection matrix
   */
  public SubspaceProjectionResult(int correlationDimensionality, Matrix similarityMat) {
    super();
    this.correlationDimensionality = correlationDimensionality;
    this.similarityMat = similarityMat;
  }

  @Override
  public int getCorrelationDimension() {
    return correlationDimensionality;
  }

  @Override
  public Matrix similarityMatrix() {
    return similarityMat;
  }
}
