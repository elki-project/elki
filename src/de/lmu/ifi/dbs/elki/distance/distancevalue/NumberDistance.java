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

/**
 * Provides a Distance for a number-valued distance.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf Number
 * 
 * @param <D> the (final) type of NumberDistance used
 * @param <N> the type of Number used (e.g. Double, Integer, Float, etc.)
 */
public abstract class NumberDistance<D extends NumberDistance<D, N>, N extends Number> extends AbstractDistance<D> {
  /**
   * Constructs a new NumberDistance object that represents the value argument.
   */
  public NumberDistance() {
    super();
  }

  /**
   * Build a new instance from a double value.
   * 
   * @param val Value
   * @return Distance
   */
  public abstract D fromDouble(double val);

  /**
   * Get the value as double.
   * 
   * @return same result as getValue().doubleValue() but may be more efficient.
   */
  public abstract double doubleValue();

  /**
   * Get the value as float.
   * 
   * @return same result as getValue().floatValue() but may be more efficient.
   */
  public float floatValue() {
    return (float) doubleValue();
  }

  /**
   * Get the value as int.
   * 
   * @return same result as getValue().intValue() but may be more efficient.
   */
  public int intValue() {
    return (int) longValue();
  }

  /**
   * Get the value as long.
   * 
   * @return same result as getValue().longValue() but may be more efficient.
   */
  public abstract long longValue();

  /**
   * Get the value as short.
   * 
   * @return same result as getValue().shortValue() but may be more efficient.
   */
  public short shortValue() {
    return (short) longValue();
  }

  /**
   * Get the value as byte.
   * 
   * @return same result as getValue().byteValue() but may be more efficient.
   */
  public byte byteValue() {
    return (byte) longValue();
  }
}
