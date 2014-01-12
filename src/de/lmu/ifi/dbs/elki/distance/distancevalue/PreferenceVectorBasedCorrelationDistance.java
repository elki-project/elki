package de.lmu.ifi.dbs.elki.distance.distancevalue;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * A PreferenceVectorBasedCorrelationDistance holds additionally to the
 * CorrelationDistance the common preference vector of the two objects defining
 * the distance.
 * 
 * @author Elke Achtert
 */
public class PreferenceVectorBasedCorrelationDistance extends CorrelationDistance<PreferenceVectorBasedCorrelationDistance> {
  /**
   * The static factory instance
   */
  public static final PreferenceVectorBasedCorrelationDistance FACTORY = new PreferenceVectorBasedCorrelationDistance();

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1;

  /**
   * The dimensionality of the feature space (needed for serialization).
   */
  private int dimensionality;

  /**
   * The common preference vector of the two objects defining this distance.
   */
  private long[] commonPreferenceVector;

  /**
   * Empty constructor for serialization purposes.
   */
  public PreferenceVectorBasedCorrelationDistance() {
    super();
  }

  /**
   * Constructs a new CorrelationDistance object.
   * 
   * @param dimensionality the dimensionality of the feature space (needed for
   *        serialization)
   * @param correlationValue the correlation dimension to be represented by the
   *        CorrelationDistance
   * @param euclideanValue the euclidean distance to be represented by the
   *        CorrelationDistance
   * @param commonPreferenceVector the common preference vector of the two
   *        objects defining this distance
   */
  public PreferenceVectorBasedCorrelationDistance(int dimensionality, int correlationValue, double euclideanValue, long[] commonPreferenceVector) {
    super(correlationValue, euclideanValue);
    this.dimensionality = dimensionality;
    this.commonPreferenceVector = commonPreferenceVector;
  }

  /**
   * Returns the common preference vector of the two objects defining this
   * distance.
   * 
   * @return the common preference vector
   */
  public long[] getCommonPreferenceVector() {
    return commonPreferenceVector;
  }

  /**
   * Returns a string representation of this
   * PreferenceVectorBasedCorrelationDistance.
   * 
   * @return the correlation value, the euclidean value and the common
   *         preference vector separated by blanks
   */
  @Override
  public String toString() {
    return super.toString() + SEPARATOR + commonPreferenceVector.toString();
  }

  /**
   * Checks if the dimensionality values of this distance and the specified
   * distance are equal. If the check fails an IllegalArgumentException is
   * thrown, otherwise
   * {@link CorrelationDistance#compareTo(CorrelationDistance)
   * CorrelationDistance#compareTo(distance)} is returned.
   * 
   * @return the value of
   *         {@link CorrelationDistance#compareTo(CorrelationDistance)
   *         CorrelationDistance#compareTo(distance)}
   * @throws IllegalArgumentException if the dimensionality values of this
   *         distance and the specified distance are not equal
   */
  @Override
  public int compareTo(PreferenceVectorBasedCorrelationDistance distance) {
    if(this.dimensionality >= 0 && distance.dimensionality >= 0 && this.dimensionality != distance.dimensionality) {
      throw new IllegalArgumentException("The dimensionality values of this distance " + "and the specified distance need to be equal.\n" + "this.dimensionality     " + this.dimensionality + "\n" + "distance.dimensionality " + distance.dimensionality + "\n");
    }

    return super.compareTo(distance);
  }

  @Override
  public Pattern getPattern() {
    return CORRELATION_DISTANCE_PATTERN;
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance parseString(String pattern) throws IllegalArgumentException {
    if(pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if(testInputPattern(pattern)) {
      String[] values = SEPARATOR.split(pattern);
      return new PreferenceVectorBasedCorrelationDistance(-1, Integer.parseInt(values[0]), FormatUtil.parseDouble(values[1]), null);
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance infiniteDistance() {
    return new PreferenceVectorBasedCorrelationDistance(-1, Integer.MAX_VALUE, Double.POSITIVE_INFINITY, new long[0]);
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance nullDistance() {
    return new PreferenceVectorBasedCorrelationDistance(-1, 0, 0, new long[0]);
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance undefinedDistance() {
    return new PreferenceVectorBasedCorrelationDistance(-1, -1, Double.NaN, new long[0]);
  }
}