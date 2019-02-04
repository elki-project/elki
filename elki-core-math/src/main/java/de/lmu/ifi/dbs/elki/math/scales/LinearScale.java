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
package de.lmu.ifi.dbs.elki.math.scales;

import java.text.NumberFormat;
import java.util.Locale;

import net.jafama.FastMath;

/**
 * Class to handle a linear scale for an axis.
 * 
 * The computed scales are rounded to be on decimal borders, choosing an
 * appropriate resolution to have between 4 and 31 major tics (3 to 30
 * intervals). Future versions might use major/minor tics to get even nicer
 * values.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 */
// TODO: iterator over sensible tics (major/minor)
// TODO: interface for logarithmic scales
// TODO: magic to choose appropriate linear/log scales based on data
// distribution.
public class LinearScale {
  /**
   * Maximum number of steps at which to decrease the resolution.
   */
  public static final int MAXTICKS = 31;

  /**
   * Resulting factor.
   */
  private final double ZOOMFACTOR = Math.log10(MAXTICKS);

  /**
   * min value of the scale
   */
  private double min;

  /**
   * max value of the scale
   */
  private double max;

  /**
   * Scale resolution
   */
  private double res;

  /**
   * Scale resolution in log10.
   */
  private int log10res;

  /**
   * Scale delta := max - min
   */
  private double delta;

  /**
   * Constructor. Computes a scale covering the range of min-max with between 3
   * and 30 intervals, rounded to the appropriate number of digits.
   * 
   * @param min actual minimum in the data
   * @param max actual maximum in the data
   */
  public LinearScale(double min, double max) {
    if(max < min) {
      double tmp = max;
      max = min;
      min = tmp;
    }
    this.delta = max - min;
    if(this.delta <= Double.MIN_NORMAL) {
      this.delta = 1.0;
    }
    log10res = (int) Math.ceil(Math.log10(this.delta) - ZOOMFACTOR);
    res = FastMath.pow(10, log10res);

    // round min and max according to the resolution counters
    this.min = Math.floor(min / res + .001) * res;
    this.max = Math.ceil(max / res - .001) * res;
    if(this.min == this.max) {
      this.max = this.min + res;
    }
    // Update delta (note: updated min, max!)
    this.delta = this.max - this.min;
    if(this.delta <= Double.MIN_NORMAL) {
      this.delta = 1.0;
    }
  }

  /**
   * Get minimum value (scale, not data).
   * 
   * @return min
   */
  public double getMin() {
    return min;
  }

  /**
   * Get maximum value (scale, not data).
   * 
   * @return max
   */
  public double getMax() {
    return max;
  }

  /**
   * Get resolution (scale interval size)
   * 
   * @return scale interval size
   */
  public double getRes() {
    return res;
  }

  /**
   * Get resolution (scale interval size)
   * 
   * @return scale interval size in logarithmic form
   */
  public double getLog10Res() {
    return log10res;
  }

  /**
   * Covert a value to it's scale position
   * 
   * @param val data value
   * @return scale position in the interval [0:1]
   */
  public double getScaled(double val) {
    return (val - min) / delta;
  }

  /**
   * Covert a scale position to the actual value
   * 
   * @param val scale position in the interval [0:1]
   * @return value on the original scale
   */
  public double getUnscaled(double val) {
    return val * delta + min;
  }

  /**
   * Covert a relative value to it's scale position
   * 
   * @param val relative data value
   * @return relative scale position in the interval [0:1]
   */
  public double getRelativeScaled(double val) {
    return val / delta;
  }

  /**
   * Covert a relative scale position to the actual value
   * 
   * @param val relative scale position in the interval [0:1]
   * @return relative value on the original scale
   */
  public double getRelativeUnscaled(double val) {
    return val * delta;
  }

  /**
   * Covert a value to it's scale position
   * 
   * @param val data value
   * @param smin target scale minimum
   * @param smax target scale maximum
   * @return scale position in the interval [smin:smax]
   */
  public double getScaled(double val, double smin, double smax) {
    return getScaled(val) * (smax - smin) + smin;
  }

  /**
   * Covert a scale position to the actual value
   * 
   * @param val scale position in the interval [smin:smax]
   * @param smin target scale minimum
   * @param smax target scale maximum
   * @return value on the original scale
   */
  public double getUnscaled(double val, double smin, double smax) {
    return getUnscaled(val) * (smax - smin) + smin;
  }

  /**
   * Covert a relative value to it's scale position
   * 
   * @param val relative data value
   * @param smin target scale minimum
   * @param smax target scale maximum
   * @return relative scale position in the interval [smin:smax]
   */
  public double getRelativeScaled(double val, double smax, double smin) {
    return getRelativeScaled(val) * (smax - smin);
  }

  /**
   * Covert a relative scale position to the actual value
   * 
   * @param val relative scale position in the interval [smin:smax]
   * @param smin target scale minimum
   * @param smax target scale maximum
   * @return relative value on the original scale
   */
  public double getRelativeUnscaled(double val, double smin, double smax) {
    return getRelativeUnscaled(val) * (smax - smin);
  }

  /**
   * Format value according to the scales resolution (i.e. appropriate number of
   * digits)
   * 
   * @param val Value
   * @return formatted number
   */
  public String formatValue(double val) {
    NumberFormat fmt = NumberFormat.getInstance(Locale.US);
    fmt.setMaximumFractionDigits(-log10res);
    return fmt.format(val);
  }

  @Override
  public String toString() {
    return "LinearScale[" + min + ", " + max + "]";
  }
}