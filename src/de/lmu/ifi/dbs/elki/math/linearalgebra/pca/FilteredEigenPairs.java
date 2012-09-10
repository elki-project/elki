package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;

/**
 * Encapsulates weak and strong eigenpairs that have been filtered out
 * by an eigenpair filter.
 *
 * @author Elke Achtert
 * 
 * @apiviz.has EigenPair
 */
public class FilteredEigenPairs {
  /**
   * The weak eigenpairs.
   */
  private final List<EigenPair> weakEigenPairs;

  /**
   * The strong eigenpairs.
   */
  private final List<EigenPair> strongEigenPairs;

  /**
   * Creates a new object that encapsulates weak and strong eigenpairs
   * that have been filtered out by an eigenpair filter.
   *
   * @param weakEigenPairs the weak eigenpairs
   * @param strongEigenPairs the strong eigenpairs
   */
  public FilteredEigenPairs(List<EigenPair> weakEigenPairs, List<EigenPair> strongEigenPairs) {
    this.weakEigenPairs = weakEigenPairs;
    this.strongEigenPairs = strongEigenPairs;
  }

  /**
   * Returns the weak eigenpairs (no copy).
   * @return the weak eigenpairs
   */
  public List<EigenPair> getWeakEigenPairs() {
    return weakEigenPairs;
  }

  /**
   * Counts the strong eigenpairs.
   * @return number of strong eigenpairs
   */
  public int countWeakEigenPairs() {
    return strongEigenPairs.size();
  }

  /**
   * Returns the strong eigenpairs (no copy).
   * @return the strong eigenpairs
   */
  public List<EigenPair> getStrongEigenPairs() {
    return strongEigenPairs;
  }
  
  /**
   * Counts the strong eigenpairs.
   * @return number of strong eigenpairs
   */
  public int countStrongEigenPairs() {
    return strongEigenPairs.size();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return "weak EP: " + weakEigenPairs + "\nstrong EP: " + strongEigenPairs;
  }
}
