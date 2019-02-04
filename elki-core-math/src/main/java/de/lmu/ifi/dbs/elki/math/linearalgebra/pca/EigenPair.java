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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Helper class which encapsulates an eigenvector and its corresponding
 * eigenvalue. This class is used to sort eigenpairs.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - Matrix
 */
public class EigenPair implements Comparable<EigenPair> {
  /**
   * The eigenvector as a matrix.
   */
  private double[] eigenvector;

  /**
   * The corresponding eigenvalue.
   */
  private double eigenvalue;

  /**
   * Creates a new EigenPair object.
   * 
   * @param eigenvector the eigenvector as a matrix
   * @param eigenvalue the corresponding eigenvalue
   */
  public EigenPair(double[] eigenvector, double eigenvalue) {
    this.eigenvalue = eigenvalue;
    this.eigenvector = eigenvector;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object's eigenvalue
   * is greater than, equal to, or less than the specified object's eigenvalue.
   * 
   * @param o the Eigenvector to be compared.
   * @return a negative integer, zero, or a positive integer as this object's
   *         eigenvalue is greater than, equal to, or less than the specified
   *         object's eigenvalue.
   */
  @Override
  public int compareTo(EigenPair o) {
    if(this.eigenvalue < o.eigenvalue) {
      return -1;
    }
    if(this.eigenvalue > o.eigenvalue) {
      return +1;
    }
    return 0;
  }

  /**
   * Returns the eigenvector.
   * 
   * @return the eigenvector
   */
  public double[] getEigenvector() {
    return eigenvector;
  }

  /**
   * Returns the eigenvalue.
   * 
   * @return the eigenvalue
   */
  public double getEigenvalue() {
    return eigenvalue;
  }

  /**
   * Returns a string representation of this EigenPair.
   * 
   * @return a string representation of this EigenPair
   */
  @Override
  public String toString() {
    return "(ew = " + eigenvalue + ", ev = [" + FormatUtil.format(eigenvector) + "])";
  }
}