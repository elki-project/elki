package de.lmu.ifi.dbs.elki.distance.distancevalue;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    return correlationValue == -1 && Double.isNaN(euclideanValue);
  }
}