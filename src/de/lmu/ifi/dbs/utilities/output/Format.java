package de.lmu.ifi.dbs.utilities.output;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Provides several methods for formatting objects for print purposes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Format {
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
   * Formats the double d with 2 fraction digits.
   *
   * @param d the double to be formatted
   * @return a String representing the double d
   */
  public static String standardFormat(final double d) {
    return format(d, 2);
  }
}
