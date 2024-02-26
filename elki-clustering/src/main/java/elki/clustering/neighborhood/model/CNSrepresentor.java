package elki.clustering.neighborhood.model;

import elki.database.ids.DBIDs;

/**
 * 
 * @author Niklas Strahmann
 */
public class CNSrepresentor {
  public int size;

  public double[] cnsMean;

  public double[] elementSum;

  public DBIDs cnsElements;

  public CNSrepresentor(double[] cnsMean, double[] elementSum, int size, DBIDs cnsElements) {
    this.cnsMean = cnsMean;
    this.size = size;
    this.cnsElements = cnsElements;
    this.elementSum = elementSum; // currently unstable
  }
}
