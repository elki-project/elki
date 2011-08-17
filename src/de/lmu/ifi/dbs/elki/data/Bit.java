package de.lmu.ifi.dbs.elki.data;
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
 * Provides a bit number. The bit is internally represented as boolean.
 * 
 * @author Arthur Zimek
 */
public class Bit extends Number {
  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = 390879869314931240L;

  /**
   * Pattern defining valid bit values. A valid bit value is either 0 or 1.
   */
  public static final Pattern BIT_PATTERN = Pattern.compile("^[01]$");

  /**
   * Method to construct a Bit for a given String expression.
   * 
   * @param bit a String expression defining a Bit
   * @return a Bit as defined by the given String expression
   * @throws NumberFormatException if the given String expression does not fit
   *         to the Pattern {@link #BIT_PATTERN BIT_PATTERN}
   */
  public static Bit valueOf(String bit) throws NumberFormatException {
    if(!BIT_PATTERN.matcher(bit).matches()) {
      throw new NumberFormatException("Input \"" + bit + "\" does not fit required pattern: " + BIT_PATTERN.pattern());
    }
    return new Bit(Integer.parseInt(bit));
  }

  /**
   * Internal representation of the bit value.
   */
  private boolean bit;

  /**
   * Provides a new bit according to the specified boolean value.
   * 
   * @param bit the boolean value of this bit
   */
  public Bit(boolean bit) {
    this.bit = bit;
  }

  /**
   * Provides a new bit according to the specified integer value. The bit value
   * is 1 for true and 0 for false.
   * 
   * @param bit 1 for true and 0 for false
   * @throws IllegalArgumentException if the specified value is neither 0 nor 1.
   */
  public Bit(int bit) throws IllegalArgumentException {
    if(bit != 0 && bit != 1) {
      throw new IllegalArgumentException("Required: 0 or 1 - found: " + bit);
    }
    this.bit = bit == 1;
  }

  /**
   * Provides an integer representation of the bit.
   * 
   * @return 1 if the bit is set, 0 otherwise
   */
  @Override
  public int intValue() {
    return bit ? 1 : 0;
  }

  /**
   * Provides a long value for the integer representation of this Bit as given
   * by {@link #intValue() intValue()}.
   */
  @Override
  public long longValue() {
    return intValue();
  }

  /**
   * Provides a float value for the integer representation of this Bit as given
   * by {@link #intValue() intValue()}.
   */
  @Override
  public float floatValue() {
    return intValue();
  }

  /**
   * Provides a double value for the integer representation of this Bit as given
   * by {@link #intValue() intValue()}.
   */
  @Override
  public double doubleValue() {
    return intValue();
  }

  /**
   * Returns the bit value as a boolean.
   * 
   * @return the bit value
   */
  public boolean bitValue() {
    return this.bit;
  }

  /**
   * Provides the String representation of the integer representation of this
   * Bit as given by {@link #intValue() intValue()}.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return Integer.toString(intValue());
  }
}
