/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.data;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;

/**
 * A boolean number type.
 * 
 * @author Arthur Zimek
 * @since 0.1
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
   * True bit.
   */
  public static final Bit TRUE = new Bit(true);

  /**
   * False bit.
   */
  public static final Bit FALSE = new Bit(false);

  /**
   * Method to construct a Bit for a given String expression.
   * 
   * @param bit a String expression defining a Bit
   * @return a Bit as defined by the given String expression
   * @throws NumberFormatException if the given String expression does not fit
   *         the defined pattern.
   */
  public static Bit valueOf(String bit) throws NumberFormatException {
    final int i = ParseUtil.parseIntBase10(bit);
    if(i != 0 && i != 1) {
      throw new NumberFormatException("Input \"" + bit + "\" must be 0 or 1.");
    }
    return (i > 0) ? TRUE : FALSE;
  }

  /**
   * Convert truth value to a bit.
   * 
   * @param b Truth value
   * @return Bit
   */
  public static Bit valueOf(boolean b) {
    return b ? TRUE : FALSE;
  }

  /**
   * Internal representation of the bit value.
   */
  private boolean bit;

  /**
   * Create a new bit according to the specified boolean value.
   * 
   * @param bit the boolean value of this bit
   * 
   * @deprecated Use {@link Bit#valueOf} to save memory.
   */
  @Deprecated
  public Bit(boolean bit) {
    this.bit = bit;
  }

  /**
   * Create a new bit according to the specified integer value. The bit value
   * is 1 for true and 0 for false.
   * 
   * @param bit 1 for true and 0 for false
   * @throws IllegalArgumentException if the specified value is neither 0 nor 1.
   * @deprecated Use {@link Bit#valueOf} to save memory.
   */
  @Deprecated
  public Bit(int bit) throws IllegalArgumentException {
    if(bit != 0 && bit != 1) {
      throw new IllegalArgumentException("Required: 0 or 1 - found: " + bit);
    }
    this.bit = (bit == 1);
  }

  @Override
  public int intValue() {
    return bit ? 1 : 0;
  }

  @Override
  public long longValue() {
    return intValue();
  }

  @Override
  public float floatValue() {
    return intValue();
  }

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

  @Override
  public String toString() {
    return Integer.toString(intValue());
  }
}
