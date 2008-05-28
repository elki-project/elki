package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

/**
 * Provides a distance function that computes the Euklidean distance
 * between feature vectors only in specified dimensions.
 *
 * @author Elke Achtert
 */
public class DimensionsSelectingEuklideanDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractDimensionsSelectingDoubleDistanceFunction<V>
    implements SpatialDistanceFunction<V, DoubleDistance> {

  /**
   * Provides a distance function that computes the Euklidean distance
   * between feature vectors only in specified dimensions
   */
  public DimensionsSelectingEuklideanDistanceFunction() {
    super();
  }


  /**
   * Provides the Euklidean distance
   * between two given feature vectors in the selected dimensions.
   *
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Euklidean distance
   *         between two given feature vectors in the selected dimensions
   */
  public DoubleDistance distance(V v1, V v2) {
    if (v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  " +
                                         "first argument: " + v1 + "\n  " +
                                         "second argument: " + v2);
    }

    if (v1.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " +
                                         "is not consistent with the specified dimensions " +
                                         "to be considered for distance computation.\n  " +
                                         "dimensionality of the feature space: " + v1.getDimensionality() + "\n  " +
                                         "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    // schneller wenn man nur die set Bits durchgeht und direkt die Dimension anspringt
    // TODO unten entsprechend
    for (int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)){ 
      double manhattanI = v1.getValue(d+1).doubleValue() - v2.getValue(d+1).doubleValue();
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  @Override
  public String description() {
    return "Euklidean distance for feature vectors considering only specified dimensions. " +
           "No parameters required. " +
           "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

  /**
   * Computes the minimum distance between the given MBR and the RealVector
   * object according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the FeatureVector object
   * @return the minimum distance between the given MBR and the SpatialData
   *         object according to this distance function
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#minDist(de.lmu.ifi.dbs.utilities.HyperBoundingBox,de.lmu.ifi.dbs.data.FeatureVector)
   */
  public DoubleDistance minDist(HyperBoundingBox mbr, V o) {
    if (mbr.getDimensionality() != o.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + o.toString());
    }
    if (o.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " +
                                         "is not consistent with the specified dimensions " +
                                         "to be considered for distance computation.\n  " +
                                         "dimensionality of the feature space: " + o.getDimensionality() + "\n  " +
                                         "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for (int d = 1; d <= o.getDimensionality(); d++) {
      if (!getSelectedDimensions().get(d - 1)) continue;

      double value = o.getValue(d).doubleValue();
      double r;
      if (value < mbr.getMin(d))
        r = mbr.getMin(d);
      else if (value > mbr.getMax(d))
        r = mbr.getMax(d);
      else
        r = value;

      double manhattanI = value - r;
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }

  /**
   * Computes the minimum distance between the given MBR and the NumberVector object
   * with the given id according to this distance function.
   *
   * @param mbr the MBR object
   * @param id  the id of the NumberVector object
   * @return the minimum distance between the given MBR and the SpatialData object
   *         according to this distance function
   */
  public DoubleDistance minDist(HyperBoundingBox mbr, Integer id) {
    return minDist(mbr, getDatabase().get(id));
  }

  /**
   * Computes the distance between the two given MBRs according to this
   * distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this
   *         distance function
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#distance(de.lmu.ifi.dbs.utilities.HyperBoundingBox,de.lmu.ifi.dbs.utilities.HyperBoundingBox)
   */
  public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    if (mbr1.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " +
                                         "is not consistent with the specified dimensions " +
                                         "to be considered for distance computation.\n  " +
                                         "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " +
                                         "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for (int d = 1; d <= mbr1.getDimensionality(); d++) {
      if (!getSelectedDimensions().get(d - 1)) continue;

      double m1, m2;
      if (mbr1.getMax(d) < mbr2.getMin(d)) {
        m1 = mbr1.getMax(d);
        m2 = mbr2.getMin(d);
      }
      else if (mbr1.getMin(d) > mbr2.getMax(d)) {
        m1 = mbr1.getMin(d);
        m2 = mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        m1 = 0;
        m2 = 0;
      }
      double manhattanI = m1 - m2;
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#centerDistance(de.lmu.ifi.dbs.utilities.HyperBoundingBox,de.lmu.ifi.dbs.utilities.HyperBoundingBox)
   */
  public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    if (mbr1.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " +
                                         "is not consistent with the specified dimensions " +
                                         "to be considered for distance computation.\n  " +
                                         "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " +
                                         "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for (int d = 1; d <= mbr1.getDimensionality(); d++) {
      if (!getSelectedDimensions().get(d - 1)) continue;

      double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
      double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

      double manhattanI = c1 - c2;
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }
}
