package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;

/**
 * Provides the Euklidean distance for FeatureVectors.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class EuklideanDistanceFunction<T extends NumberVector> extends DoubleDistanceFunction<T>
implements SpatialDistanceFunction<T, DoubleDistance> {

  /**
   * Provides a Euklidean distance function that can compute the Euklidean
   * distance (that is a DoubleDistance) for FeatureVectors.
   */
  public EuklideanDistanceFunction() {
    super();
  }

  /**
   * Provides the Euklidean distance between the given two vectors.
   *
   * @return the Euklidean distance between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance distance(T rv1, T rv2) {
    if (rv1.getDimensionality() != rv2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + rv1.toString() + "\n  second argument: " + rv2.toString());
    }
    double sqrDist = 0;
    for (int i = 1; i <= rv1.getDimensionality(); i++) {
      double manhattanI = rv1.getValue(i).doubleValue() - rv2.getValue(i).doubleValue();
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return "Euklidean distance for FeatureVectors. No parameters required. Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

  /**
   * Computes the minimum distance between the given MBR and the RealVector
   * object according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the FeatureVector object
   * @return the minimum distance between the given MBR and the SpatialData
   *         object according to this distance function
   * @see SpatialDistanceFunction#minDist(HyperBoundingBox, de.lmu.ifi.dbs.data.NumberVector)
   */
  public DoubleDistance minDist(HyperBoundingBox mbr, T o) {
    if (mbr.getDimensionality() != o.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + o.toString());
    }

    double sqrDist = 0;
    for (int d = 1; d <= o.getDimensionality(); d++) {
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
   * @see SpatialDistanceFunction#distance(HyperBoundingBox, HyperBoundingBox)
   */
  public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }

    double sqrDist = 0;
    for (int d = 1; d <= mbr1.getDimensionality(); d++) {
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
   * @see SpatialDistanceFunction#centerDistance(HyperBoundingBox, de.lmu.ifi.dbs.utilities.HyperBoundingBox)
   */
  public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }

    double sqrDist = 0;
    for (int d = 1; d <= mbr1.getDimensionality(); d++) {
      double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
      double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

      double manhattanI = c1 - c2;
      sqrDist += manhattanI * manhattanI;
    }
    return new DoubleDistance(Math.sqrt(sqrDist));
  }
}
