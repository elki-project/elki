package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.FeatureVector;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector type
 */
public class MeanModel<V extends FeatureVector<V, ?>> extends BaseModel {
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
}
