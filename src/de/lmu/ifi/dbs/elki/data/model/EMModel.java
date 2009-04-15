package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model of an EM cluster, providing a mean and a full covariance Matrix.
 * 
 * @author Erich Schubert
 *
 * @param <V>
 */
public class EMModel<V extends RealVector<V, ?>> extends BaseModel {
  /**
   * Cluster mean
   */
  private V mean;
  /**
   * Cluster covariance matrix
   */
  private Matrix covarianceMatrix;

  /**
   * Constructor.
   * 
   * @param mean
   * @param covarianceMatrix
   */
  public EMModel(V mean, Matrix covarianceMatrix) {
    super();
    this.mean = mean;
    this.covarianceMatrix = covarianceMatrix;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    try {
      super.writeToText(out, label);
      out.commentPrintLn("Mean: "+out.normalizationRestore(this.mean).toString());
      out.commentPrintLn("Covariance Matrix: "+this.covarianceMatrix.toString());
    }
    catch(NonNumericFeaturesException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * @return mean
   */
  public V getMean() {
    return mean;
  }

  /**
   * @param mean
   */
  public void setMean(V mean) {
    this.mean = mean;
  }

  /**
   * @return covariance matrix
   */
  public Matrix getCovarianceMatrix() {
    return covarianceMatrix;
  }

  /**
   * @param covarianceMatrix
   */
  public void setCovarianceMatrix(Matrix covarianceMatrix) {
    this.covarianceMatrix = covarianceMatrix;
  }
}
