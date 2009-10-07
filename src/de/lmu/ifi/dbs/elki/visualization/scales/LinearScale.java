package de.lmu.ifi.dbs.elki.visualization.scales;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Class to handle a linear scale for an axis.
 * 
 * The computed scales are rounded to be on decimal borders, choosing an appropriate resolution
 * to have between 4 and 31 major tics (3 to 30 intervals). Future versions might use
 * major/minor tics to get even nicer values.
 * 
 * @author Erich Schubert
 *
 */

// TODO: iterator over sensible tics (major/minor)
// TODO: interface for logarithmic scales
// TODO: magic to choose appropriate linear/log scales based on data distribution.
public class LinearScale {
  // at 31 scale steps, decrease resolution.
  private final double ZOOMFACTOR = Math.log10(31);

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
   * Constructor.
   * Computes a scale covering the range of min-max with between 3 and 30 intervals, rounded
   * to the appropriate number of digits.
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
    double delta = max - min;
    log10res = (int) Math.ceil(Math.log10(delta) - ZOOMFACTOR);
    res = Math.pow(10, log10res);

    // round min and max according to the resolution counters
    this.min = Math.floor(min / res) * res;
    this.max = Math.ceil(max / res) * res;
    this.delta = this.max - this.min;
    
    //System.err.println(min+"~"+this.min+" "+max+"~"+this.max+" % "+this.res+" "+this.delta);
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
   * @return max
   */
  public double getMax() {
    return max;
  }

  /**
   * Get resolution (scale interval size)
   * @return scale interval size
   */
  public double getRes() {
    return res;
  }
  
  /**
   * Get resolution (scale interval size)
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
   * Format value according to the scales resolution (i.e. appropriate number of digits)
   * 
   * @param val
   * @return formatted number
   */
  public String formatValue(double val) {
    NumberFormat fmt = NumberFormat.getInstance(Locale.US);
    fmt.setMaximumFractionDigits(-log10res);
    return fmt.format(val);
  }
}