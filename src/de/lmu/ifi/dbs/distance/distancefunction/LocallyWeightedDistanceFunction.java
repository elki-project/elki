package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

/**
 * Provides a locally weighted distance function.
 * Computes the quadratic form distance between two vectors P and Q as follows:
 * result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)}
 * where dist<sub>X</sub>(X,Y) = (X-Y)*<b>M<sub>X</sub></b>*(X-Y)<b><sup>T</sup></b>
 * and <b>M<sub>X</sub></b> is the weight matrix of vector X.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LocallyWeightedDistanceFunction<O extends RealVector<O,?>,P extends Preprocessor<O>> extends AbstractLocallyWeightedDistanceFunction<O,P> implements SpatialDistanceFunction<O, DoubleDistance> {
  /**
   * Provides a locally weighted distance function.
   */
  public LocallyWeightedDistanceFunction() {
    super();
  }

  /**
   * Computes the distance between two given real vectors according to this
   * distance function.
   *
   * @param o1 first RealVector
   * @param o2 second RealVector
   * @return the distance between two given real vectors according to this
   *         distance function
   */
  public DoubleDistance distance(O o1, O o2) {
    Matrix m1 = (Matrix) getDatabase().getAssociation(AssociationID.LOCALLY_WEIGHTED_MATRIX, o1.getID());
    Matrix m2 = (Matrix) getDatabase().getAssociation(AssociationID.LOCALLY_WEIGHTED_MATRIX, o2.getID());

    if (m1 == null || m2 == null) {
      return new DoubleDistance(Double.POSITIVE_INFINITY);
    }

    //noinspection unchecked
    Matrix rv1Mrv2 = o1.plus(o2.negativeVector()).getColumnVector();
    //noinspection unchecked
    Matrix rv2Mrv1 = o2.plus(o1.negativeVector()).getColumnVector();

    double dist1 = rv1Mrv2.transpose().times(m1).times(rv1Mrv2).get(0, 0);
    double dist2 = rv2Mrv1.transpose().times(m2).times(rv2Mrv1).get(0, 0);

    if (dist1 < 0) {
      if (-dist1 < 0.000000000001) dist1 = 0;
      else throw new IllegalArgumentException("dist1 " + dist1 + "  < 0!");
    }
    if (dist2 < 0) {
      if (-dist2 < 0.000000000001) dist2 = 0;
      else throw new IllegalArgumentException("dist2 " + dist2 + "  < 0!");
    }

    return new DoubleDistance(Math.max(Math.sqrt(dist1), Math.sqrt(dist2)));
  }


  /**
   * Computes the minimum distance between the given MBR and the RealVector
   * object according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the FeatureVector object
   * @return the minimum distance between the given MBR and the SpatialData
   *         object according to this distance function
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#minDist(de.lmu.ifi.dbs.utilities.HyperBoundingBox, de.lmu.ifi.dbs.data.FeatureVector)
   */
  public DoubleDistance minDist(HyperBoundingBox mbr, O o) {
    if (mbr.getDimensionality() != o.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr.toString() + "\n  second argument: " + o.toString());
    }

    double[] r = new double[o.getDimensionality()];
    for (int d = 1; d <= o.getDimensionality(); d++) {
      double value = o.getValue(d).doubleValue();
      if (value < mbr.getMin(d))
        r[d - 1] = mbr.getMin(d);
      else if (value > mbr.getMax(d))
        r[d - 1] = mbr.getMax(d);
      else
        r[d - 1] = value;
    }

    O mbrVector = o.newInstance(r);
    Matrix m = (Matrix) getDatabase().getAssociation(AssociationID.LOCALLY_WEIGHTED_MATRIX, o.getID());
    //noinspection unchecked
    Matrix rv1Mrv2 = o.plus(mbrVector.negativeVector()).getColumnVector();
    double dist = rv1Mrv2.transpose().times(m).times(rv1Mrv2).get(0, 0);

    return new DoubleDistance(Math.sqrt(dist));
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
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#distance(HyperBoundingBox, de.lmu.ifi.dbs.utilities.HyperBoundingBox)
   */
  public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
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
   * @see de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction#centerDistance(HyperBoundingBox, de.lmu.ifi.dbs.utilities.HyperBoundingBox)
   */
  public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
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

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  AssociationID getAssociationID() {
    return AssociationID.LOCALLY_WEIGHTED_MATRIX;
  }

}
