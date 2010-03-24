package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model of an EM cluster, providing a mean and a full covariance
 * Matrix.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class EMModel<V extends FeatureVector<V, ?>> extends MeanModel<V> {
  /**
   * Cluster covariance matrix
   */
  private Matrix covarianceMatrix;

  /**
   * Constructor.
   * 
   * @param mean Mean vector
   * @param covarianceMatrix Covariance matrix
   */
  public EMModel(V mean, Matrix covarianceMatrix) {
    super(mean);
    this.covarianceMatrix = covarianceMatrix;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Mean: " + out.normalizationRestore(this.getMean()).toString());
    out.commentPrintLn("Covariance Matrix: " + this.covarianceMatrix.toString());
  }

  /**
   * @return covariance matrix
   */
  public Matrix getCovarianceMatrix() {
    return covarianceMatrix;
  }

  /**
   * @param covarianceMatrix covariance matrix
   */
  public void setCovarianceMatrix(Matrix covarianceMatrix) {
    this.covarianceMatrix = covarianceMatrix;
  }
}
