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
package de.lmu.ifi.dbs.elki.math.statistics.tests;

/**
 * Interface for the statistical test used by HiCS.
 * 
 * Consists of a single method that calculates the deviation between two data
 * samples, given as arrays of double values.
 * 
 * Note that different measures may have very different scale!
 * 
 * @author Jan Brusis
 * @author Erich Schubert
 * @since 0.5.0
 */
public interface GoodnessOfFitTest {
  /**
   * Measure the deviation of a full sample from a conditional sample.
   * 
   * Sample arrays <em>may</em> be modified, e.g. sorted, by the test.
   * 
   * @param fullSample Full sample
   * @param conditionalSample Conditional sample
   * 
   * @return Deviation
   */
  double deviation(double[] fullSample, double[] conditionalSample);
}