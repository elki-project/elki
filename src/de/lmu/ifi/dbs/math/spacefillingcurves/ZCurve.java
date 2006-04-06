package de.lmu.ifi.dbs.math.spacefillingcurves;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;
import java.util.logging.Logger;

/**
 * TODO: comment
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
  private static boolean DEBUG = true;

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

  /*
  public static <O extends NumberVector> void orderByZValue(List<O> objects) {
    if (objects.isEmpty()) return;

    // determine max value in each dimension
    int dimensionality = objects.get(0).getDimensionality();
    double[] minValues = new double[dimensionality];
    double[] maxValues = new double[dimensionality];
    Arrays.fill(minValues, Double.MAX_VALUE);
    Arrays.fill(maxValues, -Double.MAX_VALUE);
    for (O o : objects) {
      for (int d = 1; d <= dimensionality; d++) {
        double value = o.getValue(d).doubleValue();
        maxValues[d - 1] = Math.max(value, maxValues[d - 1]);
        minValues[d - 1] = Math.min(value, minValues[d - 1]);
      }
    }

    // discretise the objects and determine z-value
    final Map<Integer, Long> zValues = new HashMap<Integer,Long>();
    for (O o : objects) {
      boolean[][] bits = discretise(o, minValues, maxValues);
      zValues.put(o.getID(), getZValue(bits));
    }

    // create a comparator
    Comparator<O> comparator = new Comparator<O>() {
      public int compare(O o1, O o2) {
        long z1 = zValues.get(o1.getID());
        long z2 = zValues.get(o1.getID());

        if (z1 < z2) return -1;
        if (z1 > z2) return +1;
        return o1.getID() - o2.getID();
      }
    };

    Collections.sort(objects, comparator);
  }     */

  /**
   * Computes the z-value of the specified discrete values.
   *
   * @param discreteValues
   * @return
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
  private static <O extends NumberVector> boolean[][] discretise(List<Number> values, double[] minValues, double[] maxValues) {
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

  private static long bitsToLong(boolean[] bits) {
    long value = 0;
    for (int i = 0; i < bits.length; i++) {
      if (bits[i]) value += Math.pow(2,i);
    }
    return value;
  }


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
    for (int i = 0; i < disc.length; i++) {
      Util.format(disc[i], "");
    }

    long z = getZValue(disc);
    if (VERBOSE) {
      logger.info("\n z " + z);
    }
  }


}
