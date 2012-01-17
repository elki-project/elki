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

/**
 * An interface to define requirements for a number to perform arithmetic
 * operations. The Numbers are supposed to remain unchanged by an arithmetic
 * operation.
 * 
 * @author Arthur Zimek
 * @param <N> Number type
 */
public interface Arithmetic<N extends Number> extends Comparable<N> {
  /**
   * Adds the given number to this number.
   * 
   * @param number the number to add to this number.
   * @return the result of arithmetic addition of this Number with the given
   *         number
   */
  public N plus(N number);

  /**
   * Multiplies this number with the given number.
   * 
   * @param number the number to multiply this number with
   * @return the result of arithmetic multiplication of this Number with the
   *         given number
   */
  public N times(N number);

  /**
   * Subtracts the given number from this number.
   * 
   * @param number the number to subtract from this number
   * @return the result of arithmetic subtraction of the given number from this
   *         Number
   */
  public N minus(N number);

  /**
   * Divides this number by the given number.
   * 
   * @param number the number to divide this number by
   * @return the result of arithmetic division of this Number by the given
   *         number
   */
  public N divided(N number);
}
