package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.math.Primes;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Halton sequences are a pseudo-uniform distribution. The data is actually too
 * regular for a true uniform distribution, but as such will of course often
 * appear to be uniform.
 * 
 * Technically, they are based on Van der Corput sequence and the Von Neumann
 * Katutani transformation. These produce a series of integers which then are
 * converted to floating point values.
 * 
 * To randomize, we just choose a random starting position, as indicated by
 * 
 * Reference:
 * <p>
 * Randomized halton sequences<br>
 * X. Wang and F. J. Hickernell<br />
 * Mathematical and Computer Modelling Vol. 32 (7)
 * </p>
 * 
 * <b>Important note: this code hasn't been double checked yet. While it
 * probably works for some simple cases such as example data set generation, do
 * <em>not</em> rely on it for e.g. quasi monte carlo methods without
 * double-checking the quality, and looking at more advanced methods!</b>
 * 
 * Let me repeat this: this code was written <b>to generate toy datasets</b>. It
 * <b>may have deficits</b> for other uses! <b>There is a high chance it will
 * produce correlated data when used for more than one dimension.</b> - for toy
 * data sets, try different random seeds until you find one that works for you.
 * 
 * TODO: find an improved algorithm that takes care of a better randomization,
 * for example by adding scrambling.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
@Reference(title = "Randomized halton sequences", //
    authors = "X. Wang and F. J. Hickernell", //
    booktitle = "Mathematical and Computer Modelling Vol. 32 (7)", //
    url = "http://dx.doi.org/10.1016/S0895-7177(00)00178-3")
public class HaltonUniformDistribution implements Distribution {
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
   * Maximum number of iterations of fast variant
   */
  private static final int MAXFAST = 1000;

  /**
   * Threshold
   */
  private static final double ALMOST_ONE = 1.0 - 1e-10;

  /**
   * Base value
   */
  final short base;

  /**
   * Inverse of base, for faster division by multiplication.
   */
  final double invbase;

  /**
   * Logarithm of base.
   */
  final double logbase;

  /**
   * Maximum integer to use
   */
  final int maxi;

  /**
   * Counter, for max iterations of fast function.
   */
  int counter = 0;

  /**
   * Current value
   */
  double current;

  /**
   * Integer inverse
   */
  long inverse;

  /**
   * Constructor for a halton pseudo uniform distribution on the interval [min,
   * max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param base Base value
   * @param seed Random seed (starting value)
   */
  public HaltonUniformDistribution(double min, double max, int base, double seed) {
    super();
    // Swap parameters if they were given incorrectly.
    if(min > max) {
      double tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;

    this.base = (short) base;
    this.invbase = 1.0 / base;
    this.logbase = Math.log(base);
    // 32 bit * log(2) / log(base)
    this.maxi = (int) (32.0 * MathUtil.LOG2 / logbase);
    this.current = seed;
    this.inverse = inverse(seed);
  }

  /**
   * Constructor for a halton pseudo uniform distribution on the interval [min,
   * max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public HaltonUniformDistribution(double min, double max) {
    this(min, max, new Random());
  }

  /**
   * Constructor for a halton pseudo uniform distribution on the interval [min,
   * max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param rnd Random generator
   */
  public HaltonUniformDistribution(double min, double max, Random rnd) {
    this(min, max, choosePrime(rnd), rnd.nextDouble());
  }

  /**
   * Constructor for a halton pseudo uniform distribution on the interval [min,
   * max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param rnd Random generator
   */
  public HaltonUniformDistribution(double min, double max, RandomFactory rnd) {
    this(min, max, rnd.getRandom());
  }

  /**
   * Choose a random prime. We try to avoid the later primes, as they are known
   * to cause too correlated data.
   * 
   * @param rnd Random generator
   * @return Prime
   */
  private static int choosePrime(Random rnd) {
    return Primes.FIRST_PRIMES[rnd.nextInt(10)];
  }

  @Override
  public double pdf(double val) {
    if(val < min || val > max) {
      return 0.0;
    }
    return 1.0 / len;
  }

  @Override
  public double logpdf(double val) {
    if(!(val >= min) || val > max) {
      return Double.NEGATIVE_INFINITY;
    }
    return (len > 0.) ? Math.log(1.0 / len) : Double.POSITIVE_INFINITY;
  }

  @Override
  public double cdf(double val) {
    if(val < min) {
      return 0.0;
    }
    if(val > max) {
      return 1.0;
    }
    return (val - min) / len;
  }

  @Override
  public double quantile(double val) {
    return min + len * val;
  }

  /**
   * Compute the inverse with respect to the given base.
   * 
   * @param current Current value
   * @return Integer inverse
   */
  private long inverse(double current) {
    // Represent to base b.
    short[] digits = new short[maxi];
    int j;
    for(j = 0; j < maxi; j++) {
      current *= base;
      digits[j] = (short) current;
      current -= digits[j];
      if(current <= 1e-10) {
        break;
      }
    }
    long inv = 0;
    for(j = maxi - 1; j >= 0; j--) {
      inv = inv * base + digits[j];
    }
    return inv;
  }

  /**
   * Compute the radical inverse of i.
   * 
   * @param i Input long value
   * @return Double radical inverse
   */
  private double radicalInverse(long i) {
    double digit = 1.0 / (double) base;
    double radical = digit;
    double inverse = 0.0;
    while(i > 0) {
      inverse += digit * (double) (i % base);
      digit *= radical;
      i /= base;
    }
    return inverse;
  }

  /**
   * Compute the next radical inverse.
   * 
   * @return Next inverse
   */
  private double nextRadicalInverse() {
    counter++;
    // Do at most MAXFAST appromate steps
    if(counter >= MAXFAST) {
      counter = 0;
      inverse += MAXFAST;
      current = radicalInverse(inverse);
      return current;
    }
    // Fast approximation:
    double nextInverse = current + invbase;
    if(nextInverse < ALMOST_ONE) {
      current = nextInverse;
      return current;
    }
    else {
      double digit1 = invbase, digit2 = invbase * invbase;
      while(current + digit2 >= ALMOST_ONE) {
        digit1 = digit2;
        digit2 *= invbase;
      }
      current += (digit1 - 1.0) + digit2;
      return current;
    }
  }

  @Override
  public double nextRandom() {
    return min + nextRadicalInverse() * len;
  }

  @Override
  public String toString() {
    return "HaltonUniformDistribution(min=" + min + ", max=" + max + ")";
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

  /**
   * Parameterization class
   * 
   * TODO: allow manual parameterization of sequence parameters!
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double min, max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter minP = new DoubleParameter(UniformDistribution.Parameterizer.MIN_ID);
      if(config.grab(minP)) {
        min = minP.doubleValue();
      }

      DoubleParameter maxP = new DoubleParameter(UniformDistribution.Parameterizer.MAX_ID);
      if(config.grab(maxP)) {
        max = maxP.doubleValue();
      }
    }

    @Override
    protected HaltonUniformDistribution makeInstance() {
      return new HaltonUniformDistribution(min, max, rnd);
    }
  }
}
