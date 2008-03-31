package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a distance function that computes the distance
 * between feature vectors as the absolute difference of their values
 * in a specified dimension.
 *
 * @author Elke Achtert 
 */
public class DimensionSelectingDistanceFunction<N extends Number, O extends FeatureVector<O,N>> extends AbstractDoubleDistanceFunction<O> implements SpatialDistanceFunction<O, DoubleDistance> {

  /**
   * Option string for parameter dim.
   */
  public static final String DIM_P = "dim";

  /**
   * Description for parameter dim.
   */
  public static final String DIM_D = "a integer between 1 and the dimensionality of the " +
                                     "feature space 1 specifying the dimension to be considered " +
                                     "for distance computation.";
  /**
   * The dimension to be considered for distance computation.
   */
  private int dim;

  public DimensionSelectingDistanceFunction() {
    super();

    optionHandler.put(new IntParameter(DIM_P, DIM_D, new GreaterEqualConstraint(1)));
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   *
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public DoubleDistance distance(O o1, O o2) {
    if (dim > o1.getDimensionality() || dim > o2.getDimensionality()) {
      throw new IllegalArgumentException("Specified dimension to be considered " +
                                         "is larger that dimensionality of NumberVectors:" +
                                         "\n  first argument: " + o1.toString() +
                                         "\n  second argument: " + o2.toString() +
                                         "\n  dimension: " + dim);
    }

    double manhattan = o1.getValue(dim).doubleValue() - o2.getValue(dim).doubleValue();
    return new DoubleDistance(Math.abs(manhattan));
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("Distance within one specified dimension for NumberVectors. "));
    description.append('\n');
    return description.toString();
  }

  /**
   * Computes the minimum distance between the given MBR and the NumberVector object
   * according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the NumberVector object
   * @return the minimum distance between the given MBR and the SpatialData object
   *         according to this distance function
   */
  public DoubleDistance minDist(HyperBoundingBox mbr, O o) {
    if (dim > mbr.getDimensionality() || dim > o.getDimensionality()) {
      throw new IllegalArgumentException("Specified dimension to be considered " +
                                         "is larger that dimensionality of NumberVectors:" +
                                         "\n  first argument: " + mbr.toString() +
                                         "\n  second argument: " + o.toString() +
                                         "\n  dimension: " + dim);
    }

    double value = o.getValue(dim).doubleValue();
    double r;
    if (value < mbr.getMin(dim))
      r = mbr.getMin(dim);
    else if (value > mbr.getMax(dim))
      r = mbr.getMax(dim);
    else
      r = value;

    double manhattan = value - r;
    return new DoubleDistance(Math.abs(manhattan));
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
   * Computes the distance between the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this distance function
   */
  public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (dim > mbr1.getDimensionality() || dim > mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Specified dimension to be considered " +
                                         "is larger that dimensionality of NumberVectors:" +
                                         "\n  first argument: " + mbr1.toString() +
                                         "\n  second argument: " + mbr2.toString() +
                                         "\n  dimension: " + dim);
    }

    double m1, m2;
    if (mbr1.getMax(dim) < mbr2.getMin(dim)) {
      m1 = mbr1.getMax(dim);
      m2 = mbr2.getMin(dim);
    }
    else if (mbr1.getMin(dim) > mbr2.getMax(dim)) {
      m1 = mbr1.getMin(dim);
      m2 = mbr2.getMax(dim);
    }
    else { // The mbrs intersect!
      m1 = 0;
      m2 = 0;
    }
    double manhattan = m1 - m2;

    return new DoubleDistance(Math.abs(manhattan));
  }

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   */
  public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
    if (dim > mbr1.getDimensionality() || dim > mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Specified dimension to be considered " +
                                         "is larger that dimensionality of NumberVectors:" +
                                         "\n  first argument: " + mbr1.toString() +
                                         "\n  second argument: " + mbr2.toString() +
                                         "\n  dimension: " + dim);
    }

    double c1 = (mbr1.getMin(dim) + mbr1.getMax(dim)) / 2;
    double c2 = (mbr2.getMin(dim) + mbr2.getMax(dim)) / 2;

    double manhattan = c1 - c2;

    return new DoubleDistance(Math.abs(manhattan));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // dim
    dim = (Integer) optionHandler.getOptionValue(DIM_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the selected dimension.
   * @return the selected dimension
   */
  public int getSelectedDimension() {
    return dim;
  }
}
