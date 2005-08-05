package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.distance.Distance;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @version 0.1
 */
public final class Util {
  /**
   * Returns the maximum of the given Distances or the first, if none is
   * greater than the other one.
   *
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the maximum of the given Distances or the first, if
   *         neither is greater than the other one
   */
  public static Distance max(Distance d1, Distance d2) {
    if (d1.compareTo(d2) > 0) {
      return d1;
    }
    else if (d2.compareTo(d1) > 0) {
      return d2;
    }
    else {
      return d1;
    }
  }

  /**
   * Formats the double d with 2 fraction digits.
   *
   * @param d the double to be formatted
   * @return a String representing the double d
   */
  public static String format(final double d) {
    return format(d, 2);
  }

  /**
   * Formats the double d with the specified fraction digits.
   *
   * @param d      the double to be formatted
   * @param digits the number of fraction digits
   * @return a String representing the double d
   */
  public static String format(final double d, int digits) {
    final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    nf.setGroupingUsed(false);
    return nf.format(d);
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return format(d, ", ", 2);
  }

  /**
   * Formats the double array d with the specified separator and the specified
   * fraction digits.
   *
   * @param d      the double array to be formatted
   * @param sep    the seperator between the single values of the double array,
   *               e.g. ','
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, int digits) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1)
        buffer.append(format(d[i], digits)).append(sep);
      else
        buffer.append(format(d[i], digits));
    }
    return buffer.toString();
  }

  /**
   * Returns the prefix of the specidfied fileName (i.e. the name of the file
   * without extension).
   *
   * @param fileName the name of the file
   * @return the prefix of the specidfied fileName
   */
  public static String getFilePrefix(final String fileName) {
    final int index = fileName.lastIndexOf('.');
    if (index < 0)
      return fileName;
    return fileName.substring(0, index);
  }

  /**
   * Returns a new String array containng the same objects
   * as are contained in the given array.
   *
   * @param array an array to copy
   * @return the copied array
   */
  public static String[] copy(String[] array) {
    String[] copy = new String[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns a new double array containng the same objects
   * as are contained in the given array.
   *
   * @param array an array to copy
   * @return the copied array
   */
  public static double[] copy(double[] array) {
    double[] copy = new double[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }
}