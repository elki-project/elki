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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import net.jafama.FastMath;

/**
 * Provides an polynomial approximation bo + b1*k + b2*k^2 + ... + bp*k^p
 * for knn-distances consisting of parameters b0, ..., bp.
 *
 * @author Elke Achtert
 * @since 0.1
 */
public class PolynomialApproximation implements Externalizable {
  private static final long serialVersionUID = 1;

  /**
   * The parameters b0, ..., bp.
   */
  private double[] b;

  /**
   * Empty constructor for serialization purposes.
   */
  public PolynomialApproximation() {
	  // empty constructor
  }

  /**
   * Provides an polynomial approximation bo + b1*k + b2*k^2 + ... + bp*k^p
   * for knn-distances consisting of parameters b0, ..., bp.
   *
   * @param b the parameters b0, ..., bi
   */
  public PolynomialApproximation(double[] b) {
    this.b = b;
  }

  /**
   * Returns the parameter bp at the specified index p.
   *
   * @param p the index
   * @return the parameter bp at the specified index p
   */
  public double getB(int p) {
    return b[p];
  }

  /**
   * Returns a copy of the the array of coefficients b0, ..., bp.
   * @return the a copy of the array of coefficients b0, ..., bp
   */
  public double[] getCoefficients() {
    double[] result = new double[b.length];
    System.arraycopy(b, 0, result, 0, b.length);
    return result;
  }

  /**
   * Returns the order of the polynom.
   *
   * @return the order of the polynom
   */
  public int getPolynomialOrder() {
    return b.length;
  }

  /**
   * Returns the function value of the polynomial approximation
   * at the specified k.
   *
   * @param k the value for which the polynomial approximation should be returned
   * @return the function value of the polynomial approximation
   *         at the specified k
   */
  public double getValueAt(int k) {
    double result = 0.;
    double log_k = FastMath.log(k), acc = 1.;
    for (int p = 0; p < b.length; p++) {
      result += b[p] * acc;
      acc *= log_k;
    }
    return result;
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(b.length);
    for (double aB : b) {
      out.writeDouble(aB);
    }
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    b = new double[in.readInt()];
    for (int p = 0; p < b.length; p++) {
      b[p] = in.readDouble();
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return FormatUtil.format(b, FormatUtil.NF4);
  }
}
