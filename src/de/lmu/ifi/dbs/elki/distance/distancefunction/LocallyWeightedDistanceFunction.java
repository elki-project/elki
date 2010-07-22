package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.preprocessing.LocalProjectionPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a locally weighted distance function. Computes the quadratic form
 * distance between two vectors P and Q as follows:
 * 
 * result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)} where
 * dist<sub>X</sub>(X,Y) = (X-Y)*<b>M<sub>X</sub></b>*(X-Y)<b><sup>T</sup></b>
 * and <b>M<sub>X</sub></b> is the weight matrix of vector X.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
// FIXME: implements SpatialPrimitiveDistanceFunction<V, DoubleDistance>
public class LocallyWeightedDistanceFunction<V extends NumberVector<V, ?>, P extends LocalProjectionPreprocessor<V, ?>> extends AbstractLocallyWeightedDistanceFunction<V, P> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LocallyWeightedDistanceFunction(Parameterization config) {
    super(config);
  }

  @Override
  public DistanceQuery<V, DoubleDistance> preprocess(Database<V> database) {
    return new Instance<V, P>(database, getPreprocessor(), this);
  }
  
  /**
   * Instance of this distance for a particular database.
   * 
   * @author Erich Schubert
   *
   * @param <V> Vector type
   * @param <P> Preprocessor type
   */
  public static class Instance<V extends NumberVector<V, ?>, P extends LocalProjectionPreprocessor<V, ?>> extends AbstractPreprocessorBasedDistanceFunction.Instance<V, P, DoubleDistance> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param parent Owner distance
     */
    public Instance(Database<V> database, P preprocessor, LocallyWeightedDistanceFunction<V, P> parent) {
      super(database, preprocessor, parent);
    }

    /**
     * Computes the distance between two given real vectors according to this
     * distance function.
     * 
     * @param v1 first vector
     * @param v2 second vector
     * @return the distance between two given real vectors according to this
     *         distance function
     */
    @Override
    public DoubleDistance distance(DBID id1, DBID id2) {
      Matrix m1 = preprocessor.get(id1).similarityMatrix();
      Matrix m2 = preprocessor.get(id2).similarityMatrix();

      if(m1 == null || m2 == null) {
        return new DoubleDistance(Double.POSITIVE_INFINITY);
      }

      V v1 = database.get(id1);
      V v2 = database.get(id2);
      Vector v1Mv2 = v1.minus(v2).getColumnVector();
      Vector v2Mv1 = v2.minus(v1).getColumnVector();

      double dist1 = v1Mv2.transposeTimes(m1).times(v1Mv2).get(0, 0);
      double dist2 = v2Mv1.transposeTimes(m2).times(v2Mv1).get(0, 0);

      if(dist1 < 0) {
        if(-dist1 < 0.000000000001) {
          dist1 = 0;
        }
        else {
          throw new IllegalArgumentException("dist1 " + dist1 + "  < 0!");
        }
      }
      if(dist2 < 0) {
        if(-dist2 < 0.000000000001) {
          dist2 = 0;
        }
        else {
          throw new IllegalArgumentException("dist2 " + dist2 + "  < 0!");
        }
      }

      return new DoubleDistance(Math.max(Math.sqrt(dist1), Math.sqrt(dist2)));
    }

    //@Override
    // TODO: re-enable spatial interfaces
    public DoubleDistance minDist(HyperBoundingBox mbr, V v) {
      if(mbr.getDimensionality() != v.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr.toString() + "\n  second argument: " + v.toString());
      }

      double[] r = new double[v.getDimensionality()];
      for(int d = 1; d <= v.getDimensionality(); d++) {
        double value = v.doubleValue(d);
        if(value < mbr.getMin(d)) {
          r[d - 1] = mbr.getMin(d);
        }
        else if(value > mbr.getMax(d)) {
          r[d - 1] = mbr.getMax(d);
        }
        else {
          r[d - 1] = value;
        }
      }

      V mbrVector = v.newInstance(r);
      Matrix m = preprocessor.get(v.getID()).similarityMatrix();
      // noinspection unchecked
      Vector rv1Mrv2 = v.minus(mbrVector).getColumnVector();
      double dist = rv1Mrv2.transposeTimes(m).times(rv1Mrv2).get(0, 0);

      return new DoubleDistance(Math.sqrt(dist));
    }

    // TODO: Remove?
    //@Override
    //public DoubleDistance minDist(HyperBoundingBox mbr, DBID id) {
    //  return minDist(mbr, database.get(id));
    //}

    //@Override
    // TODO: re-enable spatial interface
    public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
      if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
      }

      double sqrDist = 0;
      for(int d = 1; d <= mbr1.getDimensionality(); d++) {
        double m1, m2;
        if(mbr1.getMax(d) < mbr2.getMin(d)) {
          m1 = mbr1.getMax(d);
          m2 = mbr2.getMin(d);
        }
        else if(mbr1.getMin(d) > mbr2.getMax(d)) {
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

    //@Override
    // TODO: re-enable spatial interface
    public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
      if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n first argument:  " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
      }

      double sqrDist = 0;
      for(int d = 1; d <= mbr1.getDimensionality(); d++) {
        double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
        double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

        double manhattanI = c1 - c2;
        sqrDist += manhattanI * manhattanI;
      }
      return new DoubleDistance(Math.sqrt(sqrDist));
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public Class<? super V> getInputDatatype() {
    return NumberVector.class;
  }
}