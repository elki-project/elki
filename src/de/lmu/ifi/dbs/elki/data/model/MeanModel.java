package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class MeanModel<V extends FeatureVector<V, ?>> extends BaseModel implements TextWriteable{
  /**
   * Cluster mean
   */
  private V mean;

  /**
   * Constructor with mean
   * 
   * @param mean Cluster mean
   */
  public MeanModel(V mean) {
    super();
    this.mean = mean;
  }

  /**
   * @return mean
   */
  public V getMean() {
    return mean;
  }

  /**
   * @param mean Mean vector
   */
  public void setMean(V mean) {
    this.mean = mean;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(TextWriterStream.SER_MARKER + " " + getClass().getName());
    out.commentPrintLn("Cluster Mean: " + mean.toString());
  }
}
