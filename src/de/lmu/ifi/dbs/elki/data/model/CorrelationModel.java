package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;

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
   * Accessor for PCA result
   * @return
   */
  public PCAFilteredResult getPCAResult() {
    return pcaresult;
  }

  /**
   * Accessor for PCA result
   * @param pcaresult
   */
  public void setPCAResult(PCAFilteredResult pcaresult) {
    this.pcaresult = pcaresult;
  }

  /**
   * Accessor for Centroid
   * @return
   */
  public V getCentroid() {
    return centroid;
  }

  /**
   * Accessor for Centroid
   * @param centroid
   */
  public void setCentroid(V centroid) {
    this.centroid = centroid;
  }

  /**
   * Implementation of {@link TextWriteable} interface
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    try {
      out.commentPrintLn("Centroid: " + out.normalizationRestore(getCentroid()).toString());
      out.commentPrintLn("Strong Eigenvectors:");
      out.commentPrintLn(getPCAResult().getStrongEigenvectors().toString());
      out.commentPrintLn("Weak Eigenvectors:");
      out.commentPrintLn(getPCAResult().getWeakEigenvectors().toString());
      out.commentPrintLn("Eigenvalues: "+Util.format(getPCAResult().getEigenvalues(), " , ", 2));
    }
    catch(NonNumericFeaturesException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
