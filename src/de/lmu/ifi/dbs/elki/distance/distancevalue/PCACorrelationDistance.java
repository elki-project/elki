package de.lmu.ifi.dbs.elki.distance.distancevalue;

import java.util.regex.Pattern;


/**
 * The correlation distance is a special Distance that indicates the
 * dissimilarity between correlation connected objects. The correlation distance
 * between two points is a pair consisting of the correlation dimension of two
 * points and the euclidean distance between the two points.
 * 
 * @author Elke Achtert
 */
public class PCACorrelationDistance extends CorrelationDistance<PCACorrelationDistance> {
  /**
   * The static factory instance
   */
  public final static PCACorrelationDistance FACTORY = new PCACorrelationDistance();
  
  /**
   * Serial
   */
  private static final long serialVersionUID = 1L;

  /**
   * Empty constructor for serialization purposes.
   */
  public PCACorrelationDistance() {
    // for serialization
  }

  /**
   * Constructs a new CorrelationDistance object consisting of the specified
   * correlation value and euclidean value.
   * 
   * @param correlationValue the correlation dimension to be represented by the
   *        CorrelationDistance
   * @param euclideanValue the euclidean distance to be represented by the
   *        CorrelationDistance
   */
  public PCACorrelationDistance(int correlationValue, double euclideanValue) {
    super(correlationValue, euclideanValue);
  }

  /**
   * Provides a distance suitable to this DistanceFunction based on the given
   * pattern.
   * 
   * @param val A pattern defining a distance suitable to this
   *        DistanceFunction
   * @return a distance suitable to this DistanceFunction based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible
   *         with the requirements of this DistanceFunction
   */
  @Override
  public PCACorrelationDistance parseString(String val) throws IllegalArgumentException {
    if(val.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if(testInputPattern(val)) {
      String[] values = SEPARATOR.split(val);
      return new PCACorrelationDistance(Integer.parseInt(values[0]), Double.parseDouble(values[1]));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + val + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public Pattern getPattern() {
    return CORRELATION_DISTANCE_PATTERN;
  }

  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  @Override
  public PCACorrelationDistance infiniteDistance() {
    return new PCACorrelationDistance(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
  }

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  @Override
  public PCACorrelationDistance nullDistance() {
    return new PCACorrelationDistance(0, 0.0);
  }

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  @Override
  public PCACorrelationDistance undefinedDistance() {
    return new PCACorrelationDistance(-1, Double.NaN);
  }
  
  @Override
  public PCACorrelationDistance plus(PCACorrelationDistance distance) {
    return new PCACorrelationDistance(this.correlationValue + distance.getCorrelationValue(), this.euclideanValue + distance.getEuclideanValue());
  }

  @Override
  public PCACorrelationDistance minus(PCACorrelationDistance distance) {
    return new PCACorrelationDistance(this.correlationValue - distance.getCorrelationValue(), this.euclideanValue - distance.getEuclideanValue());
  }

  @Override
  public boolean isInfiniteDistance() {
    return correlationValue == Integer.MAX_VALUE || euclideanValue == Double.POSITIVE_INFINITY;
  }

  @Override
  public boolean isNullDistance() {
    return correlationValue == 0 || euclideanValue == 0.0;
  }

  @Override
  public boolean isUndefinedDistance() {
    return correlationValue == -1 && euclideanValue == Double.NaN;
  }
}