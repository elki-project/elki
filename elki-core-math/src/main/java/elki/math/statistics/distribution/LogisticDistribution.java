/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Logistic distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "log" })
public class LogisticDistribution implements Distribution {
  /**
   * Parameters: location and scale
   */
  double location, scale;

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   */
  public LogisticDistribution(double location, double scale) {
    this.location = location;
    this.scale = scale;
  }

  /**
   * Get the location aparameter.
   * 
   * @return location
   */
  public double getLocation() {
    return location;
  }

  /**
   * Get the scale parameter.
   *
   * @return scale
   */
  public double getScale() {
    return scale;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, location, scale);
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return PDF
   */
  public static double pdf(double val, double loc, double scale) {
    val = Math.abs((val - loc) / scale);
    double e = FastMath.exp(-val);
    double f = 1.0 + e;
    return e / (scale * f * f);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, location, scale);
  }

  /**
   * log Probability density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return log PDF
   */
  public static double logpdf(double val, double loc, double scale) {
    val = Math.abs((val - loc) / scale);
    double f = 1.0 + FastMath.exp(-val);
    return -val - FastMath.log(scale * f * f);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, location, scale);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return CDF
   */
  public static double cdf(double val, double loc, double scale) {
    val = (val - loc) / scale;
    return 1. / (1. + FastMath.exp(-val));
  }

  /**
   * log Cumulative density function.
   * 
   * TODO: untested.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return log PDF
   */
  public static double logcdf(double val, double loc, double scale) {
    val = (val - loc) / scale;
    if(val <= 18.) {
      return -FastMath.log1p(FastMath.exp(-val));
    }
    else if(val > 33.3) {
      return val;
    }
    else {
      return val - FastMath.exp(val);
    }
  }

  /**
   * Quantile function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return Quantile
   */
  public static double quantile(double val, double loc, double scale) {
    return loc + scale * FastMath.log(val / (1. - val));
  }

  /**
   * log Quantile function.
   * 
   * TODO: untested.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @return Quantile
   */
  public static double logquantile(double val, double loc, double scale) {
    return loc + scale * (val - MathUtil.log1mexp(-val));
  }

  @Override
  public double quantile(double val) {
    return quantile(val, location, scale);
  }

  @Override
  public double nextRandom(Random random) {
    double u = random.nextDouble();
    return location + scale * FastMath.log(u / (1. - u));
  }

  @Override
  public String toString() {
    return "LogisticDistribution(location=" + location + ", scale=" + scale + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double location, scale;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(SCALE_ID) //
          .grab(config, x -> scale = x);
      new DoubleParameter(LOCATION_ID) //
          .grab(config, x -> location = x);
    }

    @Override
    public LogisticDistribution make() {
      return new LogisticDistribution(location, scale);
    }
  }
}
