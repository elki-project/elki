package de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution;
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

import java.util.Random;

/**
 * Simple uniform distribution class
 * 
 * @author Erich Schubert
 */
public final class UniformDistribution implements Distribution {
  /**
   * Minimum
   */
  private double min;

  /**
   * Maximum
   */
  private double max;

  /**
   * Len := max - min
   */
  private double len;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for a uniform distribution on the interval [min, max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param random Random generator
   */
  public UniformDistribution(double min, double max, Random random) {
    // Swap parameters if they were given incorrectly.
    if(min > max) {
      double tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;
    this.random = random;
  }

  /**
   * Return the PDF of the generators distribution
   */
  @Override
  public double explain(double val) {
    if(val < min || val >= max) {
      return 0.0;
    }
    return 1.0 / len;
  }

  /**
   * Generate a random value with the generators parameters
   */
  @Override
  public double generate() {
    return min + random.nextDouble() * len;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in describing cluster models.
   */
  @Override
  public String toString() {
    return "Uniform Distribution (min=" + min + ", max=" + max + ")";
  }

  /**
   * @return the minimum value
   */
  public double getMin() {
    return min;
  }

  /**
   * @return the maximum value
   */
  public double getMax() {
    return max;
  }
}