package de.lmu.ifi.dbs.elki.distance.distancevalue;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Provides a Distance for a double-valued distance.
 * 
 * @author Elke Achtert
 */
public class DoubleDistance extends NumberDistance<DoubleDistance, Double> {
  /**
   * Zero distance constant
   */
  public static final DoubleDistance ZERO_DISTANCE = new DoubleDistance(0.0);

  /**
   * Infinite distance constant
   */
  public static final DoubleDistance INFINITE_DISTANCE = new DoubleDistance(Double.POSITIVE_INFINITY);

  /**
   * Undefined distance constant
   */
  public static final DoubleDistance UNDEFINED_DISTANCE = new DoubleDistance(Double.NaN);

  /**
   * The static factory instance
   */
  public static final DoubleDistance FACTORY = UNDEFINED_DISTANCE;

  /**
   * The actual value.
   */
  double value;

  /**
   * Generated serialVersionUID.
   */
  private static final long serialVersionUID = 3711413449321214862L;

  /**
   * Empty constructor for serialization purposes.
   */
  public DoubleDistance() {
    super();
  }

  /**
   * Constructs a new DoubleDistance object that represents the double argument.
   * 
   * @param value the value to be represented by the DoubleDistance.
   */
  public DoubleDistance(double value) {
    super();
    this.value = value;
  }

  @Override
  public DoubleDistance fromDouble(double val) {
    return new DoubleDistance(val);
  }

  @Override
  public DoubleDistance plus(DoubleDistance distance) {
    return new DoubleDistance(this.value + distance.value);
  }

  @Override
  public DoubleDistance minus(DoubleDistance distance) {
    return new DoubleDistance(this.value - distance.value);
  }

  /**
   * Returns a new distance as the product of this distance and the given
   * distance.
   * 
   * @param distance the distance to be multiplied with this distance
   * @return a new distance as the product of this distance and the given
   *         distance
   */
  public DoubleDistance times(DoubleDistance distance) {
    return new DoubleDistance(this.value * distance.value);
  }

  /**
   * Returns a new distance as the product of this distance and the given double
   * value.
   * 
   * @param lambda the double value this distance should be multiplied with
   * @return a new distance as the product of this distance and the given double
   *         value
   */
  public DoubleDistance times(double lambda) {
    return new DoubleDistance(this.value * lambda);
  }

  /**
   * Writes the double value of this DoubleDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(this.value);
  }

  /**
   * Reads the double value of this DoubleDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    this.value = in.readDouble();
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 8 (8 Byte for a double value)
   */
  @Override
  public int externalizableSize() {
    return 8;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public long longValue() {
    return (long) value;
  }

  @Override
  public int compareTo(DoubleDistance other) {
    return Double.compare(this.value, other.value);
  }

  /**
   * An infinite DoubleDistance is based on {@link Double#POSITIVE_INFINITY
   * Double.POSITIVE_INFINITY}.
   */
  @Override
  public DoubleDistance infiniteDistance() {
    return INFINITE_DISTANCE;
  }

  /**
   * A null DoubleDistance is based on 0.
   */
  @Override
  public DoubleDistance nullDistance() {
    return ZERO_DISTANCE;
  }

  /**
   * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
   */
  @Override
  public DoubleDistance undefinedDistance() {
    return UNDEFINED_DISTANCE;
  }

  /**
   * As pattern is required a String defining a Double.
   */
  @Override
  public DoubleDistance parseString(String val) throws IllegalArgumentException {
    if (val.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if (testInputPattern(val)) {
      return new DoubleDistance(Double.parseDouble(val));
    } else {
      throw new IllegalArgumentException("Given pattern \"" + val + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public boolean isInfiniteDistance() {
    return Double.isInfinite(value);
  }

  @Override
  public boolean isNullDistance() {
    return (value <= 0.0);
  }

  @Override
  public boolean isUndefinedDistance() {
    return Double.isNaN(value);
  }

  @Override
  public Pattern getPattern() {
    return DOUBLE_PATTERN;
  }

  @Override
  public String toString() {
    return FormatUtil.NF8.format(value);
  }

  @Override
  public int hashCode() {
    final long bits = Double.doubleToLongBits(value);
    return (int) (bits ^ (bits >>> 32));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DoubleDistance other = (DoubleDistance) obj;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
      return false;
    }
    return true;
  }
}
