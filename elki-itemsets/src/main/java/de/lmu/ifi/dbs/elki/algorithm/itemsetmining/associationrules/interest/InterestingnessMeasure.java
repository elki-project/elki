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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;

/**
 * Interface for interestingness measures.
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
public interface InterestingnessMeasure {
  /**
   * Computes the value of the measure for a given support values
   * 
   * @param t Total number of transaction
   * @param sX Support of the antecedent
   * @param sY Support of the consequent
   * @param sXY Support of the union of antecedent and consequent
   * @return value of the measure
   */
  double measure(int t, int sX, int sY, int sXY);
}
