package de.lmu.ifi.dbs.elki.math.statistics;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabelarizes the values for student distribution.
 * 
 * @author Elke Achtert
 */
public class StudentDistribution {
  /**
   * Available alpha values.
   */
  public static double _6000 = 0.6;

  /**
   * Available alpha values.
   */
  public static double _8000 = 0.8;

  /**
   * Available alpha values.
   */
  public static double _9000 = 0.9;

  /**
   * Available alpha values.
   */
  public static double _9500 = 0.95;

  /**
   * Available alpha values.
   */
  public static double _9750 = 0.975;

  /**
   * Available alpha values.
   */
  public static double _9900 = 0.99;

  /**
   * Available alpha values.
   */
  public static double _9950 = 0.995;

  /**
   * Available alpha values.
   */
  public static double _9990 = 0.999;

  /**
   * Available alpha values.
   */
  public static double _9995 = 0.9995;

  /**
   * Available alpha values.
   */
  public static double _4000 = 0.4;

  /**
   * Available alpha values.
   */
  public static double _2000 = 0.2;

  /**
   * Available alpha values.
   */
  public static double _1000 = 0.1;

  /**
   * Available alpha values.
   */
  public static double _0500 = 0.05;

  /**
   * Available alpha values.
   */
  public static double _0250 = 0.025;

  /**
   * Available alpha values.
   */
  public static double _0100 = 0.01;

  /**
   * Available alpha values.
   */
  public static double _0050 = 0.005;

  /**
   * Available alpha values.
   */
  public static double _0010 = 0.001;

  /**
   * Available alpha values.
   */
  public static double _0005 = 0.005;

  /**
   * Holds the t-values.
   */
  private static Map<Integer, Map<Double, Double>> tValues = new HashMap<Integer, Map<Double, Double>>();

  static {
    put(31, new double[] { 0.2533, 0.8416, 1.2816, 1.6449, 1.96, 2.3263, 2.5758, 3.0903, 3.2906 });
  }

  /**
   * Returns the t-value for the given alpha-value and degree of freedom.
   * 
   * @param alpha the alpha value
   * @param n the degree of freedom
   * @return the t-value for the given alpha-value and degree of freedom
   */
  public static double tValue(double alpha, int n) {
    if(n > 30) {
      n = 31;
    }
    Map<Double, Double> map = tValues.get(n);
    if(map == null) {
      throw new IllegalArgumentException("t-values for n=" + n + " not yet tabularized!");
    }

    Double value = map.get(alpha);
    if(value == null) {
      throw new IllegalArgumentException("t-values for alpha=" + alpha + " not tabularized!");
    }

    return value;
  }

  /**
   * Stores the specified t-values for the given degree of freedom.
   * 
   * @param n the degree of freedom
   * @param values the t-values
   */
  private static void put(int n, double[] values) {
    Map<Double, Double> map = new HashMap<Double, Double>();
    map.put(_6000, values[0]);
    map.put(_8000, values[1]);
    map.put(_9000, values[2]);
    map.put(_9500, values[3]);
    map.put(_9750, values[4]);
    map.put(_9900, values[5]);
    map.put(_9950, values[6]);
    map.put(_9990, values[7]);
    map.put(_9995, values[8]);

    map.put(_4000, -values[0]);
    map.put(_2000, -values[1]);
    map.put(_1000, -values[2]);
    map.put(_0500, -values[3]);
    map.put(_0250, -values[4]);
    map.put(_0100, -values[5]);
    map.put(_0050, -values[6]);
    map.put(_0010, -values[7]);
    map.put(_0005, -values[8]);
    tValues.put(n, map);
  }
}