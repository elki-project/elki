package de.lmu.ifi.dbs.math.spacefillingcurves;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Computes the z-values for specified long values.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ZCurve {
  /**
   * The logger of this class.
   */
  private static Logger logger = Logger.getLogger(ZCurve.class.getName());

  /**
   * The debug flag for this class.
   */
  private static boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The verbose flag for this class.
   */
  private static boolean VERBOSE = true;

  public static List<Long> zValues(List<List<Number>> valuesList) {
    if (valuesList.isEmpty()) return new ArrayList<Long>();

    // determine min and max value in each dimension
    int dimensionality = valuesList.get(0).size();
    double[] minValues = new double[dimensionality];
    double[] maxValues = new double[dimensionality];
    Arrays.fill(minValues, Double.MAX_VALUE);
    Arrays.fill(maxValues, -Double.MAX_VALUE);
    for (List<Number> values : valuesList) {
      for (int d = 1; d <= dimensionality; d++) {
        double value = values.get(d).doubleValue();
        maxValues[d - 1] = Math.max(value, maxValues[d - 1]);
        minValues[d - 1] = Math.min(value, minValues[d - 1]);
      }
    }

    // discretise the values and determine z-value
    final List<Long> zValues = new ArrayList<Long>();
    for (List<Number> values : valuesList) {
      boolean[][] bits = discretise(values, minValues, maxValues);
      zValues.add(getZValue(bits));
    }

    return zValues;
  }

  /**
   * Computes the z-value of the specified discrete values.
   *
   * @param discreteValues
   * @return the z-value of the specified discrete values
   */
  public static long getZValue(boolean[][] discreteValues) {
    boolean[] z = new boolean[64];
    int pos = 64;
    for (int j = discreteValues[0].length - 1; j >= 0; j--) {
      for (boolean[] discreteValue : discreteValues) {
        z[--pos] = discreteValue[j];
      }
    }

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      for (int i = 0; i < discreteValues.length; i++) {
        msg.append("\nbit_").append(i).append("  ").append(Util.format(discreteValues[i], ""));
      }
      msg.append("\nz bits " + Util.format(z, ""));
      logger.fine(msg.toString());
    }

    return bitsToLong(z);
  }


  /**
   * Converts the double values of the specified object
   * to discrete values represented by <code>64/object.dimensionality()</code>
   * bits.
   *
   * @param values    the feature vector
   * @param minValues the minimum values of the feature space in each dimension
   * @param maxValues the maximum values of the feature space in each dimension
   * @return int[] with the index of the assigned intervals.
   */
  private static boolean[][] discretise(List<Number> values, double[] minValues, double[] maxValues) {
    int bits = 64 / values.size();
    boolean[][] discreteValues = new boolean[values.size()][];
    int k = (int) Math.pow(2, bits);

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nnumber of bits ").append(bits);
      msg.append(" -> max int value  ").append(k);
      msg.append("\n");
      logger.fine(msg.toString());
    }

    for (int d = 1; d <= values.size(); d++) {
      double value = values.get(d).doubleValue();
      if (value == maxValues[d - 1]) {
        int discreteIntValue = (int) ((value - minValues[d - 1]) / (maxValues[d - 1] - minValues[d - 1]) * k) - 1;
        discreteValues[d - 1] = integerToBits(discreteIntValue, bits);
      }
      else {
        int discreteIntValue = (int) ((value - minValues[d - 1]) / (maxValues[d - 1] - minValues[d - 1]) * k);
        discreteValues[d - 1] = integerToBits(discreteIntValue, bits);
      }
    }

    return discreteValues;
  }

  /**
   * Transforms the specified integer value into a bit value.
   *
   * @param value the integer value
   * @param n     the maximum number of bits
   * @return the bit value of the specified integer value
   */
  private static boolean[] integerToBits(int value, int n) {
    boolean[] bits = new boolean[n];

    int pos = n;
    int i = value;
    do {
      bits[--pos] = (i % 2) == 1;
      i /= 2;
    }
    while (i != 0);

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nvalue ").append(value).append(" ").append(Integer.toBinaryString(value));
      msg.append("\nvalue ").append(value).append(" ").append(Util.format(bits, ""));
      msg.append("\n");
      logger.fine(msg.toString());
    }

    return bits;
  }

  /**
   * Transforms the specified bit value into a long value.
   *
   * @param bits the bit value
   * @return the long value of the specified bit value
   */
  private static long bitsToLong(boolean[] bits) {
    long value = 0;
    for (int i = 0; i < bits.length; i++) {
      if (bits[i]) value += Math.pow(2, i);
    }
    return value;
  }


  /**
   * For test purposes.
   *
   * @param args
   */
  public static void main(String[] args) {
    LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);

    Number[] d = new Double[]{1.0, 2.0, 3.0, 4.0, 5.0};
    if (VERBOSE) {
      logger.info("\n d " + Arrays.asList(d));
    }

    double[] minValues = new double[]{1, 1, 1, 1, 1};
    double[] maxValues = new double[]{5, 5, 5, 5, 5};

    boolean[][] disc = discretise(Arrays.asList(d), minValues, maxValues);
    if (VERBOSE) {
      System.out.println(" disc ");
    }
    for (boolean[] aDisc : disc) {
      Util.format(aDisc, "");
    }

    long z = getZValue(disc);
    if (VERBOSE) {
      logger.info("\n z " + z);
    }
  }


}
