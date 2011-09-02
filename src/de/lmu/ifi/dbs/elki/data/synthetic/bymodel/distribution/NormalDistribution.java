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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Simple generator for a Gaussian = Normal Distribution
 * 
 * @author Erich Schubert
 */
public final class NormalDistribution implements Distribution {
  /**
   * Mean value for the generator
   */
  private double mean;

  /**
   * Standard deviation
   */
  private double stddev;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for Gaussian generator
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, Random random) {
    this.mean = mean;
    this.stddev = stddev;
    this.random = random;
  }

  /**
   * Standardized Gaussian PDF
   * 
   * @param x query value
   * @return probability density
   */
  // TODO: make a math.distributions package with various PDF, CDF, Error
  // functions etc.?
  private static double phi(double x) {
    return Math.exp(-x * x / 2) / MathUtil.SQRTTWOPI;
  }

  /**
   * Gaussian distribution PDF
   * 
   * @param x query value
   * @param mu mean
   * @param sigma standard distribution
   * @return probability density
   */
  public static double phi(double x, double mu, double sigma) {
    return phi((x - mu) / sigma) / sigma;
  }

  /**
   * Return the PDF of the generators distribution
   */
  @Override
  public double explain(double val) {
    return phi(val, mean, stddev);
  }

  /**
   * Generate a random value with the generators parameters
   */
  @Override
  public double generate() {
    return mean + random.nextGaussian() * stddev;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "Normal Distribution (mean="+mean+", stddev="+stddev+")";
  }

  /**
   * @return the mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * @return the standard deviation
   */
  public double getStddev() {
    return stddev;
  }
}
