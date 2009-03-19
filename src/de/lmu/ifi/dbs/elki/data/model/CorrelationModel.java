package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Cluster model using a filtered PCA result and an centroid.
 * 
 * @author Erich Schubert
 *
 * @param <V>
 */
public class CorrelationModel<V extends RealVector<V, ?>> extends BaseModel implements TextWriteable {
  /**
   * The computed PCA result of this cluster.
   */
  private PCAFilteredResult pcaresult;

  /**
   * The centroid of this cluster.
   */
  private V centroid;

  /**
   * Constructor
   * 
   * @param pcaresult
   * @param centroid
   */
  public CorrelationModel(PCAFilteredResult pcaresult, V centroid) {
    super();
    this.pcaresult = pcaresult;
    this.centroid = centroid;
  }

  /**
   * Get assigned PCA result
   * @return PCA result
   */
  public PCAFilteredResult getPCAResult() {
    return pcaresult;
  }

  /**
   * Assign new PCA result
   * @param pcaresult
   */
  public void setPCAResult(PCAFilteredResult pcaresult) {
    this.pcaresult = pcaresult;
  }

  /**
   * Get assigned for Centroid
   * @return centroid
   */
  public V getCentroid() {
    return centroid;
  }

  /**
   * Assign new Centroid
   * @param centroid
   */
  public void setCentroid(V centroid) {
    this.centroid = centroid;
  }

  /**
   * Implementation of {@link TextWriteable} interface
   * @param label 
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    out.commentPrintLn(TextWriterStream.SER_MARKER+" " + CorrelationModel.class.getName());
    try {
      out.commentPrintLn("Centroid: " + out.normalizationRestore(getCentroid()).toString());
      out.commentPrintLn("Strong Eigenvectors:");
      String strong = getPCAResult().getStrongEigenvectors().toString();
      while (strong.endsWith("\n")) {
        strong = strong.substring(0, strong.length()-1);
      }
      out.commentPrintLn(strong);
      out.commentPrintLn("Weak Eigenvectors:");
      String weak = getPCAResult().getWeakEigenvectors().toString();
      while (weak.endsWith("\n")) {
        weak = weak.substring(0, weak.length()-1);
      }
      out.commentPrintLn(weak);
      out.commentPrintLn("Eigenvalues: "+FormatUtil.format(getPCAResult().getEigenvalues(), " ", 2));
    }
    catch(NonNumericFeaturesException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
