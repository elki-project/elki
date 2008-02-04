package de.lmu.ifi.dbs.utilities.output;

import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Provides several methods for formatting objects for print purposes.
 *
 * @author Elke Achtert 
 */
public class Format {
  /**
   * Number Formatter for output purposes.
   */
  public static final NumberFormat NF2 = NumberFormat.getInstance(Locale.US);
  public static final NumberFormat NF4 = NumberFormat.getInstance(Locale.US);
  public static final NumberFormat NF8 = NumberFormat.getInstance(Locale.US);

  static {
    NF2.setMinimumFractionDigits(2);
    NF2.setMaximumFractionDigits(2);
    NF4.setMinimumFractionDigits(4);
    NF4.setMaximumFractionDigits(4);
    NF8.setMinimumFractionDigits(8);
    NF8.setMaximumFractionDigits(8);
  }

  /**
   * Formats the double d with the specified fraction digits.
   *
   * @param d      the double array to be formatted
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
   * Formats the double array d with ', ' as separator.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return format(d, ", ");
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
      if (i < d.length - 1) {
        buffer.append(format(d[i], digits)).append(sep);
      }
      else {
        buffer.append(format(d[i], digits));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified separator.
   *
   * @param d   the double array to be formatted
   * @param sep the seperator between the single values of the double array,
   *            e.g. ','
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1) {
        buffer.append(d[i]).append(sep);
      }
      else {
        buffer.append(d[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with ', ' as separator
   * and with the specified fraction digits.
   *
   * @param d      the double array to be formatted
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[] d, int digits) {
    return format(d, ", ", digits);
  }

  /**
   * Returns an integer-string for the given input, that has as many leading
   * zeros as to match the length of the specified maximum.
   *
   * @param input   an integer to be formatted
   * @param maximum the maximum to adapt the format to
   * @return an integer-string for the given input, that has as many leading
   *         zeros as to match the length of the specified maximum
   */
  public static String format(int input, int maximum) {
    NumberFormat formatter = NumberFormat.getIntegerInstance();
    formatter.setMinimumIntegerDigits(Integer.toString(maximum).length());
    return formatter.format(input);
  }

  /**
   * Formats the hyper bounding box with ', ' as separator
   * and with the specified fraction digits.
   *
   * @param box the box to be formatted
   * @return a String representing the hyper bounding box
   */
  public static String format(HyperBoundingBox box, int digits) {
    return "min:[" +
           format(box.getMin(), ", ", digits) +
           "], max:[" +
           format(box.getMax(), ", ", digits) +
           "]";
  }

  /**
   * Returns a string with the specified number of blanks.
   *
   * @param n the nuber of blanks
   * @return a string with the specified number of blanks
   */
  public static String blanks(int n) {
    StringBuffer result = new StringBuffer(n);
    for (int i = 0; i < n; i++) {
      result.append(" ");
    }
    return result.toString();
  }
}
