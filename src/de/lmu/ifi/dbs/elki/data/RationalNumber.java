package de.lmu.ifi.dbs.elki.data;

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

import java.math.BigInteger;

/**
 * RationalNumber represents rational numbers in arbitrary precision. Note that
 * the best possible precision is the primary objective of this class. Since
 * numerator and denominator of the RationalNumber are represented as
 * BigIntegers, the required space can grow unlimited. Also arithmetic
 * operations are considerably less efficient compared to the operations with
 * doubles.
 * 
 * @author Arthur Zimek
 */
public class RationalNumber extends Number implements Arithmetic<RationalNumber> {
  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = 7347098153261459646L;

  /**
   * The canonical representation of zero as RationalNumber.
   */
  public static final RationalNumber ZERO = new RationalNumber(BigInteger.ZERO, BigInteger.ONE);

  /**
   * The canonical representation of 1 as RationalNumber.
   */
  public static final RationalNumber ONE = new RationalNumber(BigInteger.ONE, BigInteger.ONE);

  /**
   * Holding the numerator of the RationalNumber.
   */
  private BigInteger numerator;

  /**
   * Holding the denominator of the RationalNumber.
   */
  private BigInteger denominator;

  /**
   * Constructs a RationalNumber for a given numerator and denominator. The
   * denominator must not be 0.
   * 
   * @param numerator the numerator of the RationalNumber
   * @param denominator the denominator of the RationalNumber
   * @throws IllegalArgumentException if {@link BigInteger#equals(Object)
   *         denominator.equals(}{@link BigInteger#ZERO BigInteger.ZERO)}
   */
  public RationalNumber(final BigInteger numerator, final BigInteger denominator) {
    if(denominator.equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException("denominator is 0");
    }
    this.numerator = new BigInteger(numerator.toByteArray());
    this.denominator = new BigInteger(denominator.toByteArray());
    normalize();
  }

  /**
   * Constructs a RationalNumber for a given numerator and denominator. The
   * denominator must not be 0.
   * 
   * @param numerator the numerator of the RationalNumber
   * @param denominator the denominator of the RationalNumber
   * @throws IllegalArgumentException if {@link BigInteger#equals(Object)
   *         denominator.equals(}{@link BigInteger#ZERO BigInteger.ZERO)}
   */
  public RationalNumber(final long numerator, final long denominator) throws IllegalArgumentException {
    if(denominator == 0) {
      throw new IllegalArgumentException("denominator is 0");
    }
    this.numerator = BigInteger.valueOf(numerator);
    this.denominator = BigInteger.valueOf(denominator);
    normalize();
  }

  /**
   * Constructs a RationalNumber out of the given double number.
   * 
   * @param number a double number to be represented as a RationalNumber
   * @throws IllegalArgumentException if the given Double is infinit or not a
   *         number
   */
  public RationalNumber(final Double number) throws IllegalArgumentException {
    this(number.toString());
  }

  /**
   * Constructs a RationalNumber for a given String representing a double.
   * 
   * @param doubleString a String representing a double number
   * @throws IllegalArgumentException if the given String represents a double
   *         number that is infinit or not a number
   */
  public RationalNumber(final String doubleString) throws IllegalArgumentException {
    try {
      Double number = Double.parseDouble(doubleString);
      if(number.isInfinite()) {
        throw new IllegalArgumentException("given number is infinite");
      }
      if(number.isNaN()) {
        throw new IllegalArgumentException("given number is NotANumber");
      }
      // ensure standard encoding of the double argument
      String standardDoubleString = number.toString();
      // index of decimal point '.'
      int pointIndex = standardDoubleString.indexOf('\u002E');
      // read integer part
      String integerPart = pointIndex == -1 ? standardDoubleString : standardDoubleString.substring(0, pointIndex);
      // index of power 'E'
      int powerIndex = standardDoubleString.indexOf('\u0045');
      // read fractional part
      String fractionalPart = powerIndex == -1 ? standardDoubleString.substring(pointIndex + 1) : standardDoubleString.substring(pointIndex + 1, powerIndex);
      // read power
      int power = powerIndex == -1 ? 0 : Integer.parseInt(standardDoubleString.substring(powerIndex + 1));
      // concatenate integer part and fractional part to numerator
      numerator = new BigInteger(integerPart + fractionalPart);

      // reduce power accordingly to the shift of the fraction point
      power -= fractionalPart.length();
      denominator = BigInteger.ONE;
      // translate power notation
      StringBuffer multiplicandString = new StringBuffer("1");
      for(int i = 0; i < Math.abs(power); i++) {
        multiplicandString.append('0');
      }
      BigInteger multiplicand = new BigInteger(multiplicandString.toString());
      if(power < 0) {
        denominator = denominator.multiply(multiplicand);
      }
      else if(power > 0) {
        numerator = numerator.multiply(multiplicand);
      }
      normalize();
    }
    catch(NumberFormatException e) {
      throw new IllegalArgumentException("Illegal format of given number: " + doubleString);
    }
  }

  /**
   * Normalizes the RationalNumber by normalizing the signum and canceling both,
   * numerator and denominator, by the greatest common divisor.
   * <p/>
   * If the numerator is zero, the denominator is always one.
   */
  protected void normalize() {
    if(numerator.equals(BigInteger.ZERO)) {
      denominator = BigInteger.ONE;
    }
    else {
      // normalize signum
      normalizeSignum();
      // greatest common divisor
      BigInteger gcd = numerator.gcd(denominator);
      // cancel
      numerator = numerator.divide(gcd);
      denominator = denominator.divide(gcd);
    }
  }

  /**
   * Normalizes the signum such that if the RationalNumber is negative, the
   * numerator will be negative, the denominator positive. If the RationalNumber
   * is positive, both, the numerator and the denominator will be positive.
   */
  protected void normalizeSignum() {
    int numeratorSignum = numerator.signum();
    int denominatorSignum = denominator.signum();
    if(numeratorSignum == denominatorSignum) {
      if(numeratorSignum < 0) {
        numerator = numerator.abs();
        denominator = denominator.abs();
      }
    }
    else {
      if(denominatorSignum < 0) {
        numerator = numerator.negate();
        denominator = denominator.negate();
      }
    }
  }

  /**
   * Returns the integer value of {@code this.doubleValue()}.
   * 
   * @see #doubleValue()
   */
  @Override
  public int intValue() {
    return (int) doubleValue();
  }

  /**
   * Returns the long value of {@code this.doubleValue()}.
   * 
   * @see #doubleValue()
   */
  @Override
  public long longValue() {
    return (long) doubleValue();
  }

  /**
   * Returns the float value of {@code this.doubleValue()}.
   * 
   * @see #doubleValue()
   */
  @Override
  public float floatValue() {
    return (float) doubleValue();
  }

  /**
   * Returns the byte value of {@code this.doubleValue()}.
   * 
   * @see #doubleValue()
   */
  @Override
  public byte byteValue() {
    return ((Double) doubleValue()).byteValue();
  }

  /**
   * Returns the short value of {@code this.doubleValue()}.
   * 
   * @see #doubleValue()
   */
  @Override
  public short shortValue() {
    return ((Double) doubleValue()).shortValue();
  }

  /**
   * Returns the double value representation of this RationalNumber.
   * <p/>
   * The result is given by double division as
   * <code>numerator.doubleValue() / denominator.doubleValue()</code>. Note that
   * the result may not be exact. Thus after
   * <code>RationalNumber a = new RationalNumber(b.doubleValue())</code>,
   * <code>a.equals(b)</code> is not necessarily true.
   */
  @Override
  public double doubleValue() {
    return numerator.doubleValue() / denominator.doubleValue();
  }

  @Override
  public RationalNumber plus(final RationalNumber number) {
    BigInteger newNumerator = numerator.multiply(number.denominator).add(number.numerator.multiply(denominator));
    BigInteger newDenominator = denominator.multiply(number.denominator);
    return new RationalNumber(newNumerator, newDenominator);
  }

  @Override
  public RationalNumber times(final RationalNumber number) {
    BigInteger newNumerator = numerator.multiply(number.numerator);
    BigInteger newDenominator = denominator.multiply(number.denominator);
    return new RationalNumber(newNumerator, newDenominator);
  }

  @Override
  public RationalNumber minus(final RationalNumber number) {
    return plus(number.additiveInverse());
  }

  /**
   * @throws ArithmeticException if the given divisor is 0
   */
  @Override
  public RationalNumber divided(final RationalNumber number) throws ArithmeticException {
    return times(number.multiplicativeInverse());
  }

  /**
   * Returns the multiplicative inverse of this RationalNumber if it exists.
   * 
   * @return the multiplicative inverse of this rational number
   * @throws ArithmeticException if numerator is 0 and hence the multiplicative
   *         inverse of this rational number does not exist
   */
  public RationalNumber multiplicativeInverse() throws ArithmeticException {
    try {
      return new RationalNumber(denominator, numerator);
    }
    catch(IllegalArgumentException e) {
      throw new ArithmeticException("construction of inverse not possible for " + this);
    }
  }

  /**
   * Returns the additive inverse of this RationalNumber.
   * 
   * @return the additive inverse of this RationalNumber
   */
  public RationalNumber additiveInverse() {
    return new RationalNumber(numerator.negate(), denominator);
  }

  /**
   * Returns the absolute value of this rational number.
   * 
   * @return the absolute value of this rational number
   */
  public RationalNumber absValue() {
    if(this.compareTo(RationalNumber.ZERO) >= 0) {
      return this;
    }
    else {
      return this.additiveInverse();
    }
  }

  /**
   * Compares two RationalNumbers a/b and c/d. Result is the same as
   * <code>(a*d).compareTo(c*b)</code>.
   */
  @Override
  public int compareTo(final RationalNumber o) {
    BigInteger left = numerator.multiply(o.denominator);
    BigInteger right = o.numerator.multiply(denominator);

    return left.compareTo(right);
  }

  /**
   * Two RationalNumbers are considered to be equal if both denominators and
   * numerators are equal, respectively.
   */
  @Override
  public boolean equals(Object obj) {
    RationalNumber r = (RationalNumber) obj;

    return denominator.equals(r.denominator) && numerator.equals(r.numerator);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((denominator == null) ? 0 : denominator.hashCode());
    result = prime * result + ((numerator == null) ? 0 : numerator.hashCode());
    return result;
  }

  /**
   * Returns a String representation of this RationalNumber.
   * <p/>
   * The representation consists of the numerator, a separating &quot; / &quot;,
   * and the denominator of the RationalNumber.
   */
  @Override
  public String toString() {
    return numerator.toString() + " / " + denominator.toString();
  }

  /**
   * Provides a deep copy of this RationalNumber.
   * 
   * @return a deep copy of this RationalNumber
   */
  public RationalNumber copy() {
    return new RationalNumber(numerator, denominator);
  }
}